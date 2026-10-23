package org.cajapersonal.fixmyshot

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import androidx.exifinterface.media.ExifInterface
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.cajapersonal.fixmyshot.codec.LosslessEncoder
import org.cajapersonal.fixmyshot.core.*
import org.cajapersonal.fixmyshot.review.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.RandomAccessFile
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import java.util.zip.CRC32

@RunWith(AndroidJUnit4::class)
class PayloadCopyTest {
    private val app = InstrumentationRegistry.getInstrumentation().targetContext
    private val heap = Runtime.getRuntime().maxMemory()
    private fun fixture(): File {
        val id = UUID.randomUUID().toString().replace("-", "")
        val file = File(File(app.cacheDir, "review-sources").apply { mkdirs() }, "$id.source")
        val bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        try {
            bitmap.setHasAlpha(false); bitmap.eraseColor(Color.BLUE)
            bitmap.setPixel(3, 4, Color.RED)
            file.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        } finally { bitmap.recycle() }
        return file
    }
    private fun pngChunks(file: File): List<Pair<String, ByteArray>> = RandomAccessFile(file, "r").use { input ->
        input.readLong()
        val result = mutableListOf<Pair<String, ByteArray>>()
        while (input.filePointer < input.length()) {
            val length = input.readInt()
            val tag = ByteArray(4).also { input.readFully(it) }.toString(Charsets.US_ASCII)
            result += tag to ByteArray(length).also { input.readFully(it) }
            input.readInt()
        }
        result
    }
    private fun writePng(file: File, chunks: List<Pair<String, ByteArray>>) {
        java.io.DataOutputStream(file.outputStream()).use { output ->
            output.writeLong(0x89504e470d0a1a0aUL.toLong())
            for ((tag, payload) in chunks) {
                val bytes = tag.toByteArray(Charsets.US_ASCII)
                val crc = CRC32().apply { update(bytes); update(payload) }
                output.writeInt(payload.size); output.write(bytes); output.write(payload); output.writeInt(crc.value.toInt())
            }
        }
    }
    private fun request() = EncodeRequest(PixelSize(64, 64))
    private fun execute(file: File, format: OutputFormat, directory: File): PrivateExecutionResult.Ready {
        val hash = MessageDigest.getInstance("SHA-256").digest(file.readBytes()).joinToString("") { "%02x".format(it.toInt() and 255) }
        val image = DraftImage(file.name.removeSuffix(".source"), "synthetic-${file.name}", PixelSize(64, 64), false, contentSha256 = hash)
        val draft = DraftSession(destination = DestinationChoice.Custom.of(1, 1),
            batch = EditSettings(background = BackgroundChoice.White, format = format)).addImages(listOf(image))
        val recipe = ExportPreparation.inspect(draft, Build.VERSION.SDK_INT, heap).candidates.single()
        val result = PrivateOutputExecutor(ReviewSources(app), LosslessEncoder(directory, heap), heap, directory).execute(draft, recipe)
        assertTrue(result.toString(), result is PrivateExecutionResult.Ready)
        result as PrivateExecutionResult.Ready
        assertTrue(ReviewSources(app).matchesFingerprint(image.id, hash))
        assertTrue(PrivateCandidateVerifier.accepts(result.file, recipe.request))
        return result
    }

    @Test fun pngRemovesExifAndTextWithoutChangingCompressedImagePayload() {
        val file = fixture()
        val directory = File(app.cacheDir, "payload-png-" + UUID.randomUUID())
        try {
            ExifInterface(file).apply {
                setAttribute(ExifInterface.TAG_ARTIST, "Synthetic private author")
                setLatLong(12.0, 34.0); saveAttributes()
            }
            val original = pngChunks(file)
            writePng(file, listOf(original.first(), "tEXt" to "Comment\u0000private text".toByteArray()) + original.drop(1))
            val inputPayload = pngChunks(file).filter { it.first == "IDAT" }.flatMap { it.second.toList() }.toByteArray()
            val result = execute(file, OutputFormat.PNG, directory)
            assertTrue(result.payloadPreserved)
            val outputChunks = pngChunks(result.file)
            assertFalse(outputChunks.any { it.first in setOf("eXIf", "tEXt", "iTXt", "zTXt") })
            assertArrayEquals(inputPayload, outputChunks.filter { it.first == "IDAT" }.flatMap { it.second.toList() }.toByteArray())
            assertNull(ExifInterface(result.file).getAttribute(ExifInterface.TAG_ARTIST))
            assertNull(ExifInterface(result.file).latLong)
            val decoded = BitmapFactory.decodeFile(result.file.path)!!
            try { assertEquals(Color.RED, decoded.getPixel(3, 4)); assertEquals(Color.BLUE, decoded.getPixel(0, 0)) }
            finally { decoded.recycle() }
        } finally { file.delete(); directory.deleteRecursively() }
    }

    @Test fun losslessWebpKeepsVp8lBytesWhileRemovingExtendedMetadata() {
        if (Build.VERSION.SDK_INT < 30) return
        val file = fixture()
        val directory = File(app.cacheDir, "payload-webp-" + UUID.randomUUID())
        try {
            val bitmap = BitmapFactory.decodeFile(file.path)!!
            try { file.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSLESS,100,it)) } }
            finally { bitmap.recycle() }
            val payload = RandomAccessFile(file,"r").use { input ->
                input.seek(12); var found: ByteArray? = null
                while(input.filePointer<input.length()) {
                    val tag=ByteArray(4).also { input.readFully(it) }.toString(Charsets.US_ASCII)
                    val size=Integer.reverseBytes(input.readInt())
                    val bytes=ByteArray(size).also { input.readFully(it) }
                    if(size and 1!=0) input.readByte()
                    if(tag=="VP8L") found=bytes
                }; requireNotNull(found)
            }
            RandomAccessFile(file,"rw").use { output ->
                output.setLength(0);output.writeBytes("RIFF");output.writeInt(0);output.writeBytes("WEBP")
                fun chunk(tag:String,bytes:ByteArray){output.writeBytes(tag);output.writeInt(Integer.reverseBytes(bytes.size));output.write(bytes);if(bytes.size and 1!=0)output.writeByte(0)}
                chunk("VP8X",byteArrayOf(4,0,0,0,63,0,0,63,0,0))
                chunk("VP8L",payload);chunk("XMP ","<private>identity</private>".toByteArray())
                val length=output.length();output.seek(4);output.writeInt(Integer.reverseBytes((length-8).toInt()))
            }
            val result=execute(file,OutputFormat.WEBP_LOSSLESS,directory)
            assertTrue(result.payloadPreserved)
            RandomAccessFile(result.file,"r").use { input ->
                input.seek(12);input.skipBytes(18)
                assertEquals("VP8L",ByteArray(4).also { input.readFully(it) }.toString(Charsets.US_ASCII))
                assertEquals(payload.size,Integer.reverseBytes(input.readInt()))
                assertArrayEquals(payload,ByteArray(payload.size).also { input.readFully(it) })
                if(payload.size and 1!=0) input.readByte()
                assertEquals(input.length(),input.filePointer)
            }
        } finally { file.delete(); directory.deleteRecursively() }
    }

    @Test fun noncanonicalColorBadCrcAndCancellationNeverLeaveACopyCandidate() {
        val file=fixture();val target=File(app.cacheDir,"rejected-copy-"+UUID.randomUUID()+".png")
        try {
            val original=pngChunks(file)
            writePng(file,listOf(original.first(),"gAMA" to byteArrayOf(0,1,0,0))+original.filter { it.first!="gAMA" }.drop(1))
            assertFalse(PayloadPreservingCopy.write(file,target,request(),AtomicBoolean(false)))
            assertFalse(target.exists())
            writePng(file,original)
            RandomAccessFile(file,"rw").use { it.seek(it.length()-1);it.writeByte(1) }
            assertFalse(PayloadPreservingCopy.write(file,target,request(),AtomicBoolean(false)))
            assertFalse(target.exists())
            writePng(file,original)
            assertTrue(runCatching { PayloadPreservingCopy.write(file,target,request(),AtomicBoolean(true)) }.exceptionOrNull() is java.io.InterruptedIOException)
            assertFalse(target.exists())
        } finally { file.delete();target.delete() }
    }

    @Test fun pngExifOrientationIsAppliedOnceAndAnimatedPngIsRejected() {
        if(Build.VERSION.SDK_INT<29)return
        val file=fixture()
        val resolver=app.contentResolver
        val values=android.content.ContentValues().apply {
            put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME,"FixMyShot_fixture_${System.nanoTime()}.png")
            put(android.provider.MediaStore.MediaColumns.MIME_TYPE,"image/png")
            put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH,"Pictures/FixMyShot")
        }
        val uri=resolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values)!!
        val staged=mutableSetOf<String>()
        try {
            // Missing orientation can be synthesized as UNDEFINED=0; it must behave as identity.
            resolver.openOutputStream(uri,"wt")!!.use { it.write(file.readBytes()) }
            val plain = ReviewSources(app).stage(uri) as ReviewSources.ImportResult.Added
            staged += plain.image.id
            val identity = ReviewSources(app).decodeOriginal(plain.image.id, plain.image.orientedSize)
            try { assertEquals(Color.RED, identity.getPixel(3, 4)); assertEquals(PixelSize(64, 64), plain.image.orientedSize) }
            finally { identity.recycle() }
            val expected = listOf(3 to 4, 60 to 4, 60 to 59, 3 to 59, 4 to 3, 59 to 3, 59 to 60, 4 to 60)
            for (orientation in 1..8) {
                ExifInterface(file).apply { setAttribute(ExifInterface.TAG_ORIENTATION,orientation.toString());saveAttributes() }
                resolver.openOutputStream(uri,"wt")!!.use { it.write(file.readBytes()) }
                val added=ReviewSources(app).stage(uri) as ReviewSources.ImportResult.Added
                staged+=added.image.id
                val decoded=ReviewSources(app).decodeOriginal(added.image.id,added.image.orientedSize)
                val position=expected[orientation-1]
                try { assertEquals("orientation=$orientation",Color.RED,decoded.getPixel(position.first,position.second));assertEquals(Color.BLUE,decoded.getPixel(0,0)) }
                finally { decoded.recycle() }
            }
            val chunks=pngChunks(file)
            writePng(file,listOf(chunks.first(),"acTL" to byteArrayOf(0,0,0,2,0,0,0,0))+chunks.drop(1))
            resolver.openOutputStream(uri,"wt")!!.use { it.write(file.readBytes()) }
            assertTrue(ReviewSources(app).stage(uri) is ReviewSources.ImportResult.Rejected)
        } finally {file.delete();staged.forEach { ReviewSources(app).discard(it) };resolver.delete(uri,null,null)}
    }
}
