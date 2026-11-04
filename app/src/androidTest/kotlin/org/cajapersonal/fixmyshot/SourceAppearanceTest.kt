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
import org.cajapersonal.fixmyshot.export.OwnedOutputPort
import org.cajapersonal.fixmyshot.export.OutputJournal
import org.cajapersonal.fixmyshot.export.OutputStage
import org.cajapersonal.fixmyshot.export.SaveResult
import org.cajapersonal.fixmyshot.export.SaveFailure
import org.cajapersonal.fixmyshot.export.TransactionalOutputSaver
import android.net.Uri
import org.cajapersonal.fixmyshot.review.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.json.JSONArray
import org.json.JSONObject
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
        extra?.let { chunk(it, if (it == "cICP") byteArrayOf(9, 16, 0, 1) else byteArrayOf(0, 0, 0, 0)) }
        chunk("IDAT", deflate(bytes { repeat(64) { writeByte(0); repeat(64) {
            pixel.forEach { channel -> if (depth == 16) writeShort(channel * 257) else writeByte(channel) }
        } } }))
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
            val jpeg = withoutJpegIcc(ByteArrayOutputStream().also {
                assertTrue(bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it))
            }.toByteArray())
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
    /** Replace the encoder's profile rather than creating conflicting ICC sequence numbers. */
    private fun withoutJpegIcc(jpeg: ByteArray): ByteArray = bytes {
        require(jpeg.size >= 4 && jpeg[0] == 0xff.toByte() && jpeg[1] == 0xd8.toByte())
        write(jpeg, 0, 2)
        var offset = 2
        while (offset < jpeg.size) {
            val start = offset
            require(jpeg[offset++].toInt() and 255 == 255)
            while (offset < jpeg.size && jpeg[offset].toInt() and 255 == 255) offset++
            require(offset < jpeg.size)
            val marker = jpeg[offset++].toInt() and 255
            if (marker == 0xda) { write(jpeg, start, jpeg.size - start); return@bytes }
            require(marker !in setOf(0, 1, 0xd8, 0xd9) && marker !in 0xd0..0xd7 && offset + 2 <= jpeg.size)
            val length = ((jpeg[offset].toInt() and 255) shl 8) or (jpeg[offset + 1].toInt() and 255)
            require(length >= 2 && length <= jpeg.size - offset)
            val isIcc = marker == 0xe2 && length >= 14 &&
                String(jpeg, offset + 2, 12, Charsets.ISO_8859_1) == "ICC_PROFILE\u0000"
            offset += length
            if (!isIcc) write(jpeg, start, offset - start)
        }
        error("Generated JPEG has no scan")
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
    private fun sameAlphaAndCompositedColor(expected: Int, actual: Int, context: String = "") {
        assertEquals("$context Alpha must survive exactly", Color.alpha(expected), Color.alpha(actual))
        // ARGB8888 getPixel returns unpremultiplied channels. At low alpha, one stored
        // channel level expands into several straight-RGB levels. Compare visible energy.
        val alpha = Color.alpha(expected)
        for (shift in listOf(0, 8, 16)) {
            fun premultiplied(pixel: Int) = (((pixel ushr shift) and 255) * alpha + 127) / 255
            assertTrue("$context Premultiplied channel mismatch: expected=$expected actual=$actual shift=$shift",
                abs(premultiplied(expected) - premultiplied(actual)) <= 1)
        }
    }

    /** Synthetic failure evidence only; never connected to imported user images. */
    private fun captureAlphaFailure(file: File, decoded: Bitmap, image: DraftImage,
        draft: DraftSession, format: OutputFormat, side: Int, case: String) {
        var stored: Bitmap? = null
        var original: Bitmap? = null
        var reconstructed: Bitmap? = null
        try {
            require(side in setOf(64, 96) && format in setOf(OutputFormat.PNG, OutputFormat.WEBP_LOSSLESS))
            file.copyTo(File(app.filesDir, "g1-alpha-candidate.${format.extension}"), overwrite = true)
            stored = BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.ARGB_8888
                inPremultiplied = false
            }) ?: throw IOException("Synthetic reopen unavailable")
            val source = ReviewSources(app).decodeOriginal(image.id, image.orientedSize)
            original = source
            val render = ReviewBitmapRenderer.preview(source, image.orientedSize,
                draft.plans(Build.VERSION.SDK_INT).single(), maxLongSide = 1600).image
            reconstructed = render
            File(app.filesDir, "g1-alpha-rendered.png").outputStream().use {
                if (!render.compress(Bitmap.CompressFormat.PNG, 100, it)) throw IOException("Synthetic render unavailable")
            }
            fun row(bitmap: Bitmap) = JSONArray((0 until side).map {
                bitmap.getPixel(it, side / 2).toUInt().toString(16).padStart(8, '0')
            })
            fun nativeRow(bitmap: Bitmap): JSONArray {
                require(bitmap.config == Bitmap.Config.ARGB_8888)
                val bytes = ByteBuffer.allocate(bitmap.byteCount)
                bitmap.copyPixelsToBuffer(bytes)
                return JSONArray((0 until side * 4).map {
                    bytes.get((side / 2) * bitmap.rowBytes + it).toInt() and 255
                })
            }
            val raw = requireNotNull(stored)
            val evidence = JSONObject().put("schema", 1).put("kind", "SYNTHETIC_ALPHA_FAILURE")
                .put("status", "CAPTURED").put("case", case).put("side", side).put("row", side / 2)
                .put("format", format.name).put("native_byte_order", ByteOrder.nativeOrder().toString())
                .put("reconstructed_argb", row(render)).put("default_decoded_argb", row(decoded))
                .put("unpremultiplied_decoded_argb", row(raw)).put("stored_is_premultiplied", raw.isPremultiplied)
                .put("reconstructed_native_bytes", nativeRow(render)).put("default_decoded_native_bytes", nativeRow(decoded))
            File(app.filesDir, "g1-alpha-edge.json").writeText(evidence.toString())
            println("ALPHA_FAILURE_EVIDENCE_CAPTURED")
        } catch (_: Exception) { println("ALPHA_FAILURE_EVIDENCE_UNAVAILABLE") }
        finally { stored?.recycle(); original?.recycle(); reconstructed?.recycle() }
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
                        assertTrue("source=$color mime=${BitmapFactory.Options().apply { inJustDecodeBounds = true }.let { options -> BitmapFactory.decodeFile(source.path, options); options.outMimeType }} output=$format result=$result",
                            result is PrivateExecutionResult.Ready)
                        result as PrivateExecutionResult.Ready
                        try {
                            assertFalse(result.payloadPreserved)
                            assertTrue(PrivateCandidateVerifier.accepts(result.file, recipe.request, result.opaqueWebpProven))
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
            val duplicateIcc = jpeg.copyOfRange(0, 2) + jpeg.copyOfRange(2, 6 + 14 + valid.size) + jpeg.copyOfRange(2, jpeg.size)
            val gainMap = jpeg.copyOfRange(0, 2) + bytes { writeShort(0xffe2); writeShort(6); writeBytes("MPF\u0000") } + jpeg.copyOfRange(2, jpeg.size)
            val sequence = jpeg.copyOf().apply { this[18] = 2 }
            val missingFlag = variants[2].copyOf().apply { this[20] = 0 }
            for ((data, reason) in listOf(png(badOffset) to ReviewSources.Failure.MALFORMED,
                png(oversized) to ReviewSources.Failure.MALFORMED,
                png(profile(SourceColor.DISPLAY_P3, 1.0)) to ReviewSources.Failure.COLOR_UNSUPPORTED,
                png(extra = "cICP") to ReviewSources.Failure.HDR_UNSUPPORTED,
                png(depth = 16) to ReviewSources.Failure.HIGH_PRECISION,
                gainMap to ReviewSources.Failure.HDR_UNSUPPORTED, duplicateIcc to ReviewSources.Failure.MALFORMED,
                sequence to ReviewSources.Failure.MALFORMED, missingFlag to ReviewSources.Failure.MALFORMED)) {
                file.writeBytes(data)
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(file.path, bounds)
                assertEquals(reason, assertThrows(SourceAppearance.Rejected::class.java) {
                    SourceAppearance.inspect(file, bounds)
                }.reason)
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
        for (name in listOf("g1-alpha-edge.json", "g1-alpha-candidate.png",
                "g1-alpha-candidate.webp", "g1-alpha-rendered.png")) {
            val old = File(app.filesDir, name)
            assertTrue("Stale synthetic alpha evidence could not be cleared", !old.exists() || old.delete())
        }
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
                        val case = "format=$format side=$side background=${background.javaClass.simpleName}"
                        println("ALPHA_CASE $case")
                        val draft = DraftSession(destination = DestinationChoice.Custom.of(1, 1), batch = EditSettings(
                            format = format, background = background,
                            size = OutputSizeChoice.Exact(PixelSize(side, side), allowEnlargement = true))).addImages(listOf(image))
                        val recipe = ExportPreparation.inspect(draft, Build.VERSION.SDK_INT, heap).candidates.single()
                        val result = PrivateOutputExecutor(ReviewSources(app), LosslessEncoder(directory, heap), heap, directory)
                            .execute(draft, recipe) as PrivateExecutionResult.Ready
                        try {
                            val decoded = BitmapFactory.decodeFile(result.file.path)!!
                            var stored: Bitmap? = null
                            try {
                                // Inspect stored straight channels independently of Android's
                                // default re-premultiplication; never draw this bitmap on Canvas.
                                val encoded = BitmapFactory.decodeFile(result.file.path, BitmapFactory.Options().apply {
                                    inPreferredConfig = Bitmap.Config.ARGB_8888
                                    inPremultiplied = false
                                }) ?: throw AssertionError("$case Stored pixels unavailable")
                                stored = encoded
                                assertFalse("$case Requires straight-channel decode", encoded.isPremultiplied)
                                val middle = decoded.getPixel(side / 2, side / 2)
                                val alpha = if (background == BackgroundChoice.Transparent) 128 else 255
                                fun blend(channel: Int) = if (background == BackgroundChoice.Transparent) channel else
                                    (channel * 128 + (if (background == BackgroundChoice.White) 255 else 0) * 127 + 127) / 255
                                val expected = Color.argb(alpha, blend(240), blend(24), blend(8))
                                if (background == BackgroundChoice.Transparent) {
                                    sameAlphaAndCompositedColor(expected, middle, case)
                                    assertEquals(0, Color.alpha(decoded.getPixel(0, side / 2)))
                                    assertEquals(255, Color.alpha(decoded.getPixel(side - 1, side / 2)))
                                    assertFalse(result.opaqueWebpProven)
                                } else near(expected, middle, 2)
                                // Stored transition pixels retain hue without a second decoder
                                // premultiplication being charged to the renderer's hue budget.
                                if (background == BackgroundChoice.Transparent) {
                                    for (x in side / 5..side / 3) {
                                        val edge = encoded.getPixel(x, side / 2)
                                        if (Color.alpha(edge) >= 32) sameAlphaAndCompositedColor(Color.argb(Color.alpha(edge), 240, 24, 8), edge, "$case edge=$x")
                                    }
                                }
                                val sample = ReviewSources(app).decodeOriginal(image.id, image.orientedSize)
                                try {
                                    val preview = ReviewBitmapRenderer.preview(sample, image.orientedSize, draft.plans(Build.VERSION.SDK_INT).single())
                                    try {
                                        assertEquals("$case Stored width", preview.image.width, encoded.width)
                                        assertEquals("$case Stored height", preview.image.height, encoded.height)
                                        for (y in 0 until encoded.height) for (x in 0 until encoded.width) {
                                            val reference = preview.image.getPixel(x, y)
                                            val pixel = encoded.getPixel(x, y)
                                            assertEquals("$case alpha at$x,$y", Color.alpha(reference), Color.alpha(pixel))
                                            // Fully transparent hidden RGB is outside the output contract.
                                            if (Color.alpha(reference) != 0) assertEquals(
                                                "$case Lossless stored RGBA at$x,$y", reference, pixel)
                                            sameAlphaAndCompositedColor(reference, decoded.getPixel(x, y),
                                                "$case Default decode at$x,$y")
                                        }
                                        if (background == BackgroundChoice.Transparent) sameAlphaAndCompositedColor(preview.image.getPixel(side / 2, side / 2), middle, "$case preview")
                                        else near(preview.image.getPixel(side / 2, side / 2), middle, 2)
                                    }
                                    finally { preview.image.recycle() }
                                } finally { sample.recycle() }
                            } catch (failure: AssertionError) {
                                captureAlphaFailure(result.file, decoded, image, draft, format, side, case)
                                throw failure
                            } finally { stored?.recycle(); decoded.recycle() }
                        } finally { result.file.delete() }
                    }
                }
            }
            assertTrue(ReviewSources(app).matchesFingerprint(image.id, image.contentSha256!!))
        } finally { source.delete(); directory.deleteRecursively() }
    }

    @Test fun opaqueWebpElisionNeedsFullCanvasProofAndSurvivesTransactionalSave() {
        val check = Bitmap.createBitmap(4097, 3, Bitmap.Config.ARGB_8888)
        try {
            check.eraseColor(Color.BLUE)
            check.setHasAlpha(true)
            assertTrue(PrivateCandidateVerifier.isFullyOpaque(check))
            check.setPixel(4096, 2, Color.argb(128, 240, 24, 8))
            assertFalse("Last pixel outside the first tile must prohibit elision", PrivateCandidateVerifier.isFullyOpaque(check))
            assertThrows(java.io.InterruptedIOException::class.java) {
                PrivateCandidateVerifier.isFullyOpaque(check, AtomicBoolean(true))
            }
        } finally { check.recycle() }
        val (image, source) = fixture(png())
        val directory = File(app.cacheDir, "opaque-webp-${UUID.randomUUID()}")
        try {
            val draft = DraftSession(destination = DestinationChoice.Custom.of(1, 1), batch = EditSettings(
                format = OutputFormat.WEBP_LOSSLESS, background = BackgroundChoice.Transparent)).addImages(listOf(image))
            val recipe = ExportPreparation.inspect(draft, Build.VERSION.SDK_INT, heap).candidates.single()
            val result = PrivateOutputExecutor(ReviewSources(app), LosslessEncoder(directory, heap), heap, directory)
                .execute(draft, recipe)
            assertTrue(result.toString(), result is PrivateExecutionResult.Ready)
            result as PrivateExecutionResult.Ready
            try {
                assertTrue(result.opaqueWebpProven)
                assertTrue(PrivateCandidateVerifier.accepts(result.file, recipe.request, result.opaqueWebpProven))
                val decoded = BitmapFactory.decodeFile(result.file.path)!!
                val elided = try { !decoded.hasAlpha() } finally { decoded.recycle() }
                var creates = 0
                var stored: ByteArray? = null
                val port = object : OwnedOutputPort {
                    override fun create(name: String, mime: String): Uri { creates++; return Uri.parse("content://synthetic/opaque-webp") }
                    override fun openWrite(uri: Uri): java.io.OutputStream = object : ByteArrayOutputStream() {
                        override fun close() { stored = toByteArray(); super.close() }
                    }
                    override fun openRead(uri: Uri): java.io.InputStream? = stored?.inputStream()
                    override fun publish(uri: Uri): Boolean = stored != null
                    override fun delete(uri: Uri): Boolean { stored = null; return true }
                }
                val journal = OutputJournal(File(directory, "published.json"))
                val saver = TransactionalOutputSaver(app.cacheDir, port, journal)
                if (elided) {
                    assertFalse(PrivateCandidateVerifier.accepts(result.file, recipe.request))
                    assertEquals(SaveResult.Failed(SaveFailure.INVALID_CANDIDATE),
                        saver.save(recipe, result.copy(opaqueWebpProven = false)))
                    assertEquals(0, creates); assertNull(journal.read())
                }
                assertTrue(saver.save(recipe, result) is SaveResult.Published)
                assertEquals(1, creates); assertEquals(OutputStage.PUBLISHED, journal.read()!!.stage)
                assertArrayEquals(result.file.readBytes(), stored)
                assertTrue(ReviewSources(app).matchesFingerprint(image.id, image.contentSha256!!))
                assertFalse(PrivateCandidateVerifier.accepts(result.file, recipe.request.copy(hasAlpha = false), true))
            } finally { result.file.delete() }
        } finally { source.delete(); directory.deleteRecursively() }
    }
}
