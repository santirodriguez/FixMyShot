package org.cajapersonal.fixmyshot

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import android.provider.MediaStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.cajapersonal.fixmyshot.codec.LosslessEncoder
import org.cajapersonal.fixmyshot.codec.EncodeResult
import org.cajapersonal.fixmyshot.core.*
import org.cajapersonal.fixmyshot.export.ExportDraftJournal
import org.cajapersonal.fixmyshot.export.FrozenPlanFingerprint
import org.cajapersonal.fixmyshot.export.ReviewSessionJournal
import org.cajapersonal.fixmyshot.export.ReviewSessionSnapshot
import org.cajapersonal.fixmyshot.review.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import java.util.UUID
import java.util.zip.CRC32
import java.util.zip.DeflaterOutputStream
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt

/** Generated color patches only. No real photos or physical-display acceptance. */
@RunWith(AndroidJUnit4::class)
class SourceAppearanceTest {
    private val app = InstrumentationRegistry.getInstrumentation().targetContext
    private val heap = Runtime.getRuntime().maxMemory()
    private val srgb = doubleArrayOf(.436075, .222505, .013932, .385065, .716879, .097105, .143080, .060616, .714173)
    private val p3 = doubleArrayOf(.515102, .241182, -.001049, .291965, .692236, .041882, .157153, .066581, .784378)
    private val adobe = doubleArrayOf(.609756, .311124, .019481, .205240, .625656, .060890, .149224, .063220, .744839)
    private val pixel = intArrayOf(204, 128, 64)
    private fun bytes(block: DataOutputStream.() -> Unit) = ByteArrayOutputStream().also {
        DataOutputStream(it).use(block)
    }.toByteArray()
    private fun deflate(data: ByteArray) = ByteArrayOutputStream().also { out ->
        DeflaterOutputStream(out).use { it.write(data) }
    }.toByteArray()
    private fun profile(color: SourceColor, gamma: Double? = null): ByteArray {
        fun xyz(values: DoubleArray) = bytes {
            writeBytes("XYZ "); writeInt(0); values.forEach { writeInt((it * 65536).roundToInt()) }
        }
        val matrix = when (color) { SourceColor.DISPLAY_P3 -> p3; SourceColor.ADOBE_RGB -> adobe; else -> srgb }
        val curve = bytes {
            writeBytes("para"); writeInt(0)
            if (gamma != null || color == SourceColor.ADOBE_RGB) {
                writeShort(0); writeShort(0); writeInt(((gamma ?: (563.0 / 256)) * 65536).roundToInt())
            } else {
                writeShort(4); writeShort(0)
                listOf(2.4, 1 / 1.055, .055 / 1.055, 1 / 12.92, .04045, 0.0, 0.0)
                    .forEach { writeInt((it * 65536).roundToInt()) }
            }
        }
        val tags = linkedMapOf("wtpt" to xyz(doubleArrayOf(.9642, 1.0, .8249)))
        for ((index, channel) in listOf("r", "g", "b").withIndex()) {
            tags[channel + "XYZ"] = xyz(matrix.copyOfRange(index * 3, index * 3 + 3))
            tags[channel + "TRC"] = curve
        }
        val start = 132 + 12 * tags.size
        val size = start + tags.values.sumOf { (it.size + 3) / 4 * 4 }
        val result = ByteBuffer.allocate(size).order(ByteOrder.BIG_ENDIAN)
        result.putInt(size); result.position(8); result.putInt(0x04300000)
        result.put("mntrRGB XYZ ".toByteArray()); result.position(24)
        listOf(2026, 1, 1, 0, 0, 0).forEach { result.putShort(it.toShort()) }
        result.put("acsp".toByteArray()); result.position(64); result.putInt(1)
        listOf(.9642, 1.0, .8249).forEach { result.putInt((it * 65536).roundToInt()) }
        result.position(128); result.putInt(tags.size)
        var offset = start
        for ((name, payload) in tags) {
            result.put(name.toByteArray()); result.putInt(offset); result.putInt(payload.size)
            System.arraycopy(payload, 0, result.array(), offset, payload.size)
            offset += (payload.size + 3) / 4 * 4
        }
        return result.array()
    }
    private fun png(profile: ByteArray? = null, extra: String? = null, depth: Int = 8): ByteArray = bytes {
        fun chunk(name: String, data: ByteArray) {
            val tag = name.toByteArray()
            val crc = CRC32().apply { update(tag); update(data) }
            writeInt(data.size); write(tag); write(data); writeInt(crc.value.toInt())
        }
        writeLong(0x89504e470d0a1a0aUL.toLong())
        chunk("IHDR", bytes { writeInt(64); writeInt(64); writeByte(depth); writeByte(2); write(byteArrayOf(0, 0, 0)) })
        profile?.let { chunk("iCCP", "Synthetic\u0000\u0000".toByteArray() + deflate(it)) }
        extra?.let { chunk(it, byteArrayOf(0, 0, 0, 0)) }
        chunk("IDAT", deflate(bytes { repeat(64) { writeByte(0); repeat(64) { pixel.forEach { channel -> writeByte(channel) } } } }))
        chunk("IEND", byteArrayOf())
    }
    private fun fixture(data: ByteArray): Pair<DraftImage, File> {
        val id = UUID.randomUUID().toString().replace("-", "")
        val file = File(File(app.cacheDir, "review-sources").apply { mkdirs() }, "$id.source")
        file.writeBytes(data)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        val color = SourceAppearance.inspect(file, bounds)
        val hash = MessageDigest.getInstance("SHA-256").digest(data).joinToString("") { "%02x".format(it.toInt() and 255) }
        return DraftImage(id, "synthetic-$id", PixelSize(64, 64), true, contentSha256 = hash, sourceColor = color) to file
    }
    private fun containerVariants(profile: ByteArray, directory: File): List<ByteArray> {
        val bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        try {
            bitmap.setHasAlpha(false); bitmap.eraseColor(Color.rgb(pixel[0], pixel[1], pixel[2]))
            val jpeg = ByteArrayOutputStream().also { assertTrue(bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)) }.toByteArray()
            val icc = "ICC_PROFILE\u0000".toByteArray() + byteArrayOf(1, 1) + profile
            val taggedJpeg = jpeg.copyOfRange(0, 2) + bytes {
                writeShort(0xffe2); writeShort(icc.size + 2); write(icc)
            } + jpeg.copyOfRange(2, jpeg.size)
            val encoded = LosslessEncoder(directory, heap).encode(bitmap,
                EncodeRequest(PixelSize(64, 64), OutputFormat.WEBP_LOSSLESS, hasAlpha = false), AtomicBoolean(false), 120_000)
                as EncodeResult.Encoded
            val webp = try { encoded.file.readBytes() } finally { encoded.file.delete() }
            val chunks = ByteArrayOutputStream()
            DataOutputStream(chunks).use { out ->
                fun chunk(name: String, payload: ByteArray) {
                    out.writeBytes(name); out.writeInt(Integer.reverseBytes(payload.size)); out.write(payload)
                    if (payload.size and 1 != 0) out.writeByte(0)
                }
                chunk("VP8X", byteArrayOf(32, 0, 0, 0, 63, 0, 0, 63, 0, 0))
                chunk("ICCP", profile)
                var offset = 12
                while (offset < webp.size) {
                    val length = ByteBuffer.wrap(webp, offset + 4, 4).order(ByteOrder.LITTLE_ENDIAN).int
                    val end = offset + 8 + length + (length and 1)
                    val kind = String(webp, offset, 4, Charsets.US_ASCII)
                    if (kind !in setOf("VP8X", "ICCP")) out.write(webp, offset, end - offset)
                    offset = end
                }
            }
            val taggedWebp = bytes {
                writeBytes("RIFF"); writeInt(Integer.reverseBytes(chunks.size() + 4)); writeBytes("WEBP"); write(chunks.toByteArray())
            }
            return listOf(png(profile), taggedJpeg, taggedWebp)
        } finally { bitmap.recycle() }
    }
    private fun expected(color: SourceColor): Int {
        val linear = pixel.map { value ->
            val x = value / 255.0
            if (color == SourceColor.ADOBE_RGB) x.pow(563.0 / 256) else if (x <= .04045) x / 12.92 else ((x + .055) / 1.055).pow(2.4)
        }
        // Independent standard-primary RGB transforms, not BitmapFactory/Canvas as the oracle.
        val m = if (color == SourceColor.DISPLAY_P3) arrayOf(
            doubleArrayOf(1.224745, -.224904, 0.0), doubleArrayOf(-.042058, 1.042081, 0.0), doubleArrayOf(-.019642, -.078655, 1.098537))
        else arrayOf(doubleArrayOf(1.398283, -.398283, 0.0), doubleArrayOf(0.0, 1.0, 0.0), doubleArrayOf(0.0, -.042938, 1.042938))
        val channels = m.map { row ->
            val x = row.indices.sumOf { row[it] * linear[it] }.coerceIn(0.0, 1.0)
            ((if (x <= .0031308) 12.92 * x else 1.055 * x.pow(1 / 2.4) - .055) * 255).roundToInt()
        }
        return Color.rgb(channels[0], channels[1], channels[2])
    }
    private fun near(expected: Int, actual: Int, tolerance: Int = 3) {
        for (shift in listOf(0, 8, 16, 24)) assertTrue("expected=$expected actual=$actual shift=$shift",
            abs(((expected ushr shift) and 255) - ((actual ushr shift) and 255)) <= tolerance)
    }

    @Test fun recognizedWideGamutConvertsPixelsForBothOutputsAndKeepsOriginals() {
        val directory = File(app.cacheDir, "appearance-${UUID.randomUUID()}")
        try {
            for (color in listOf(SourceColor.DISPLAY_P3, SourceColor.ADOBE_RGB)) {
              for (data in containerVariants(profile(color), directory)) {
                val (image, source) = fixture(data)
                try {
                    assertEquals(color, image.sourceColor)
                    val sample = ReviewSources(app).decodeSample(image.id)
                    try { assertTrue(sample.colorSpace!!.isSrgb); near(expected(color), sample.getPixel(32, 32)) }
                    finally { sample.recycle() }
                    for (format in listOf(OutputFormat.PNG, OutputFormat.WEBP_LOSSLESS)) {
                        val draft = DraftSession(destination = DestinationChoice.Custom.of(1, 1),
                            batch = EditSettings(format = format, background = BackgroundChoice.Transparent)).addImages(listOf(image))
                        val recipe = ExportPreparation.inspect(draft, Build.VERSION.SDK_INT, heap).candidates.single()
                        val result = PrivateOutputExecutor(ReviewSources(app), LosslessEncoder(directory, heap), heap, directory)
                            .execute(draft, recipe)
                        assertTrue(result.toString(), result is PrivateExecutionResult.Ready)
                        result as PrivateExecutionResult.Ready
                        try {
                            assertFalse(result.payloadPreserved)
                            assertTrue(PrivateCandidateVerifier.accepts(result.file, recipe.request))
                            val decoded = BitmapFactory.decodeFile(result.file.path)!!
                            try { assertTrue(decoded.colorSpace!!.isSrgb); near(expected(color), decoded.getPixel(32, 32)) }
                            finally { decoded.recycle() }
                        } finally { result.file.delete() }
                    }
                    assertTrue(ReviewSources(app).matchesFingerprint(image.id, image.contentSha256!!))
                    if (Build.VERSION.SDK_INT >= 29 && data[0] == 0x89.toByte()) {
                        val values = ContentValues().apply {
                            put(MediaStore.MediaColumns.DISPLAY_NAME, "FixMyShot_synthetic_${UUID.randomUUID()}.png")
                            put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                            put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/FixMyShot")
                        }
                        val resolver = app.contentResolver
                        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)!!
                        try {
                            resolver.openOutputStream(uri, "wt")!!.use { it.write(source.readBytes()) }
                            val added = ReviewSources(app).stage(uri)
                            assertTrue(added.toString(), added is ReviewSources.ImportResult.Added)
                            added as ReviewSources.ImportResult.Added
                            try { assertEquals(color, added.image.sourceColor) }
                            finally { ReviewSources(app).discard(added.image.id) }
                        } finally { resolver.delete(uri, null, null) }
                    }
                } finally { source.delete() }
              }
            }
        } finally { directory.deleteRecursively() }
    }

    @Test fun damagedUnknownHighDepthAndHdrDescriptorsAreRejectedBeforeNormalization() {
        val valid = profile(SourceColor.DISPLAY_P3)
        val oversized = ByteArray(65 * 1024)
        val badOffset = valid.copyOf().apply { ByteBuffer.wrap(this).putInt(136, Int.MAX_VALUE) }
        val file = File(app.cacheDir, "appearance-reject-${UUID.randomUUID()}.png")
        val directory = File(app.cacheDir, "appearance-envelope-${UUID.randomUUID()}")
        try {
            val variants = containerVariants(valid, directory)
            val jpeg = variants[1]
            val gainMap = jpeg.copyOfRange(0, 2) + bytes { writeShort(0xffe2); writeShort(6); writeBytes("MPF\u0000") } + jpeg.copyOfRange(2, jpeg.size)
            val sequence = jpeg.copyOf().apply { this[18] = 2 }
            val missingFlag = variants[2].copyOf().apply { this[20] = 0 }
            for (data in listOf(png(badOffset), png(oversized), png(profile(SourceColor.DISPLAY_P3, 1.0)),
                png(extra = "cICP"), png(depth = 16), gainMap, sequence, missingFlag)) {
                file.writeBytes(data)
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(file.path, bounds)
                assertThrows(IOException::class.java) { SourceAppearance.inspect(file, bounds) }
            }
            assertThrows(IOException::class.java) { SourceAppearance.validateProfile(valid.copyOf(131)) }
        } finally { file.delete(); directory.deleteRecursively() }
    }

    @Test fun colorIdentitySurvivesEditableFrozenAndLegacyDraftsAndBlocksChangedRetry() {
        val (image, source) = fixture(png(profile(SourceColor.DISPLAY_P3)))
        val file = File(app.cacheDir, "appearance-journal-${UUID.randomUUID()}.json")
        try {
            val draft = DraftSession(destination = DestinationChoice.Custom.of(1, 1)).addImages(listOf(image))
            val encoded = ExportDraftJournal.encodeDraft(draft)
            assertEquals(draft.images, ExportDraftJournal.decodeDraft(encoded).images)
            val journal = ReviewSessionJournal(file)
            journal.write(ReviewSessionSnapshot(draft, image.id, System.currentTimeMillis()))
            assertEquals(draft, journal.read()!!.draft)
            val recipe = ExportPreparation.inspect(draft, Build.VERSION.SDK_INT, heap).candidates.single()
            assertNotEquals(FrozenPlanFingerprint.of(listOf(recipe)),
                FrozenPlanFingerprint.of(listOf(recipe.copy(sourceColor = SourceColor.SRGB))))
            val frozen = ExportDraftJournal(File(file.path + ".frozen"))
            try {
                frozen.write(draft, FrozenPlanFingerprint.of(listOf(recipe)))
                assertTrue(frozen.matchesPlan(listOf(recipe)))
                assertFalse(frozen.matchesPlan(listOf(recipe.copy(sourceColor = SourceColor.SRGB))))
            } finally { File(file.path + ".frozen").delete() }
            encoded.getJSONArray("images").getJSONObject(0).remove("color")
            assertEquals(SourceColor.SRGB, ExportDraftJournal.decodeDraft(encoded).images.single().sourceColor)
            encoded.getJSONArray("images").getJSONObject(0).put("color", "UNKNOWN")
            assertThrows(IllegalArgumentException::class.java) { ExportDraftJournal.decodeDraft(encoded) }
            journal.clear()
        } finally { source.delete(); file.delete() }
    }

    @Test fun partialAlphaSurvivesBothCodecsAndCompositesWithoutDarkOrLightHalos() {
        val bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        val directory = File(app.cacheDir, "alpha-${UUID.randomUUID()}")
        val data = try {
            for (y in 0..63) for (x in 0..63) bitmap.setPixel(x, y,
                Color.argb(if (x < 16) 0 else if (x < 48) 128 else 255, 240, 24, 8))
            ByteArrayOutputStream().also { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }.toByteArray()
        } finally { bitmap.recycle() }
        val (image, source) = fixture(data)
        try {
            for (format in listOf(OutputFormat.PNG, OutputFormat.WEBP_LOSSLESS)) {
                for (side in listOf(64, 96)) {
                    for (background in listOf(BackgroundChoice.Transparent, BackgroundChoice.White, BackgroundChoice.Black)) {
                        val draft = DraftSession(destination = DestinationChoice.Custom.of(1, 1), batch = EditSettings(
                            format = format, background = background,
                            size = OutputSizeChoice.Exact(PixelSize(side, side), allowEnlargement = true))).addImages(listOf(image))
                        val recipe = ExportPreparation.inspect(draft, Build.VERSION.SDK_INT, heap).candidates.single()
                        val result = PrivateOutputExecutor(ReviewSources(app), LosslessEncoder(directory, heap), heap, directory)
                            .execute(draft, recipe) as PrivateExecutionResult.Ready
                        try {
                            val decoded = BitmapFactory.decodeFile(result.file.path)!!
                            try {
                                val middle = decoded.getPixel(side / 2, side / 2)
                                val alpha = if (background == BackgroundChoice.Transparent) 128 else 255
                                fun blend(channel: Int) = if (background == BackgroundChoice.Transparent) channel else
                                    (channel * 128 + (if (background == BackgroundChoice.White) 255 else 0) * 127 + 127) / 255
                                near(Color.argb(alpha, blend(240), blend(24), blend(8)), middle, 2)
                                // Transition pixels keep their red hue on a transparent target.
                                if (background == BackgroundChoice.Transparent) {
                                    for (x in side / 5..side / 3) {
                                        val edge = decoded.getPixel(x, side / 2)
                                        if (Color.alpha(edge) >= 32) near(Color.argb(Color.alpha(edge), 240, 24, 8), edge, 4)
                                    }
                                }
                                val sample = ReviewSources(app).decodeOriginal(image.id, image.orientedSize)
                                try {
                                    val preview = ReviewBitmapRenderer.preview(sample, image.orientedSize, draft.plans(Build.VERSION.SDK_INT).single())
                                    try { near(preview.image.getPixel(side / 2, side / 2), middle, 2) }
                                    finally { preview.image.recycle() }
                                } finally { sample.recycle() }
                            } finally { decoded.recycle() }
                        } finally { result.file.delete() }
                    }
                }
            }
            assertTrue(ReviewSources(app).matchesFingerprint(image.id, image.contentSha256!!))
        } finally { source.delete(); directory.deleteRecursively() }
    }
}
