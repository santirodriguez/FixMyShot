package org.cajapersonal.fixmyshot

import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.cajapersonal.fixmyshot.codec.LosslessEncoder
import org.cajapersonal.fixmyshot.core.*
import org.cajapersonal.fixmyshot.export.*
import org.cajapersonal.fixmyshot.review.PrivateOutputExecutor
import org.cajapersonal.fixmyshot.review.ReviewSources
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

/** Real PNG/WebP rendering, encoding and durable transactions; bounded synthetic provider faults. */
@RunWith(AndroidJUnit4::class)
class HundredItemBatchTest {
    private val app = InstrumentationRegistry.getInstrumentation().targetContext
    private val heap = Runtime.getRuntime().maxMemory()
    private fun digest(data: ByteArray) = MessageDigest.getInstance("SHA-256").digest(data)
        .joinToString("") { "%02x".format(it.toInt() and 255) }

    private inner class Fixture {
        val root = File(app.cacheDir, "hundred-${UUID.randomUUID()}").apply { mkdirs() }
        val context = object : ContextWrapper(app) {
            override fun getApplicationContext(): Context = this
            override fun getFilesDir() = File(root, "files").apply { mkdirs() }
            override fun getCacheDir() = File(root, "cache").apply { mkdirs() }
        }
        val candidates = File(context.cacheDir, "candidates").apply { mkdirs() }
        val jobs = File(context.filesDir, "export-jobs")
        val token = BatchOutputCoordinator.newToken()
        val folder get() = File(jobs, token)
        val port = MemoryPort()
        val sources = ReviewSources(context)
        val draft: DraftSession
        init {
            val directory = File(context.cacheDir, "review-sources").apply { mkdirs() }
            val images = (0 until 100).map { index ->
                val size = PixelSize(32 + index % 3 * 4, 48 + index % 5 * 4)
                val bitmap = Bitmap.createBitmap(size.width, size.height, Bitmap.Config.ARGB_8888)
                val data = try {
                    bitmap.setHasAlpha(false)
                    bitmap.eraseColor(Color.rgb(index * 2, 255 - index * 2, index))
                    bitmap.setPixel(size.width / 2, size.height / 2, Color.BLACK)
                    ByteArrayOutputStream().also {
                        assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it))
                    }.toByteArray()
                } finally { bitmap.recycle() }
                val id = UUID.randomUUID().toString().replace("-", "")
                File(directory, "$id.source").writeBytes(data)
                DraftImage(id, "synthetic-$index", size, false, contentSha256 = digest(data),
                    overrides = ItemOverrides(format = ItemValue.Custom(
                        if (index % 2 == 0) OutputFormat.PNG else OutputFormat.WEBP_LOSSLESS)))
            }
            draft = DraftSession(destination = DestinationChoice.Custom.of(1, 1),
                batch = EditSettings(background = BackgroundChoice.White)).addImages(images)
        }
        fun coordinator(): BatchOutputCoordinator {
            val executor = PrivateOutputExecutor(sources, LosslessEncoder(candidates, heap), heap, candidates)
            return BatchOutputCoordinator(jobs,
                { ExportPreparation.inspect(it, Build.VERSION.SDK_INT, heap) },
                BatchItemProcessor(candidates,
                    { draft, recipe, cancelled -> executor.execute(draft, recipe, cancelled) },
                    { recipe, ready, journal, cancelled ->
                        TransactionalOutputSaver(context.cacheDir, port, journal).save(recipe, ready, cancelled)
                    }), port::verify)
        }
        fun assertSourcesAndCandidates() {
            assertTrue(draft.images.all { sources.matchesFingerprint(it.id, it.contentSha256!!) })
            assertEquals(0, candidates.listFiles()?.size ?: 0)
        }
        fun receipt(index: Int) = File(folder, "item-$index.json").readBytes()
        fun assertPublishedPixels() {
            val recipes = ExportPreparation.inspect(draft, Build.VERSION.SDK_INT, heap).candidates
            for ((index, recipe) in recipes.withIndex()) {
                val record = OutputJournal(File(folder, "item-$index.json")).read()!!
                assertEquals(OutputStage.PUBLISHED, record.stage)
                assertEquals(recipe.sourceId, record.itemId)
                assertEquals(recipe.request.format.mimeType, record.mime)
                assertEquals(OutputRecoveryEvidence.MATCHED, port.verify(record))
                val data = port.bytes.getValue(Uri.parse(record.uri!!))
                val decoded = BitmapFactory.decodeByteArray(data, 0, data.size)!!
                try {
                    assertEquals(recipe.canvas.width, decoded.width)
                    assertEquals(recipe.canvas.height, decoded.height)
                    assertFalse(decoded.hasAlpha())
                    val source = sources.decodeOriginal(recipe.sourceId, draft.images[index].orientedSize)
                    try {
                        val pixel = recipe.placement
                        for (y in 0 until source.height) for (x in 0 until source.width) {
                            assertEquals("Unscaled source changed at item=$index x=$x y=$y",
                                source.getPixel(x, y), decoded.getPixel(pixel.left + x, pixel.top + y))
                        }
                    } finally { source.recycle() }
                } finally { decoded.recycle() }
            }
        }
    }

    private inner class MemoryPort : OwnedOutputPort {
        val bytes = linkedMapOf<Uri, ByteArray>()
        val visible = linkedSetOf<Uri>()
        val deleted = mutableListOf<Uri>()
        var creates = 0
        var failWriteAt = -1
        var uncertainPublishAt = -1
        override fun create(name: String, mime: String): Uri =
            Uri.parse("content://fixmyshot.synthetic/hundred/${++creates}")
        override fun openWrite(uri: Uri): java.io.OutputStream {
            if (uri.lastPathSegment!!.toInt() == failWriteAt) throw IOException("Synthetic write refusal")
            return object : ByteArrayOutputStream() {
                override fun close() { bytes[uri] = toByteArray(); super.close() }
            }
        }
        override fun openRead(uri: Uri) = bytes[uri]?.inputStream()
        override fun publish(uri: Uri): Boolean {
            check(bytes.containsKey(uri))
            check(visible.add(uri)) { "Duplicate publication" }
            return uri.lastPathSegment!!.toInt() != uncertainPublishAt
        }
        override fun delete(uri: Uri): Boolean {
            check(uri !in visible) { "Published output must not be deleted" }
            deleted += uri
            bytes.remove(uri)
            return true
        }
        fun verify(record: OutputRecord): OutputRecoveryEvidence {
            val uri = record.uri?.let(Uri::parse) ?: return OutputRecoveryEvidence.MISSING
            val data = bytes[uri] ?: return OutputRecoveryEvidence.MISSING
            return when {
                digest(data) != record.digest -> OutputRecoveryEvidence.MODIFIED
                uri !in visible -> OutputRecoveryEvidence.PENDING
                else -> OutputRecoveryEvidence.MATCHED
            }
        }
    }

    @Test fun hundredMixedOutputsCancelAndExplicitRetryKeepCommittedBytesAndReceipts() {
        val f = Fixture()
        try {
            f.port.failWriteAt = 13
            val cancelled = AtomicBoolean(false)
            val first = f.coordinator().start(f.draft, f.token, cancelled) { snapshot ->
                if (snapshot.items.take(37).all {
                    it.phase in setOf(BatchItemPhase.PUBLISHED, BatchItemPhase.FAILED)
                }) cancelled.set(true)
            } as BatchRunResult.Finished
            assertEquals(100, first.snapshot.items.size)
            assertEquals(36, first.snapshot.items.count { it.phase == BatchItemPhase.PUBLISHED })
            assertEquals(1, first.snapshot.items.count { it.phase == BatchItemPhase.FAILED })
            assertEquals(63, first.snapshot.items.count { it.phase == BatchItemPhase.CANCELLED })
            assertEquals(37, f.port.creates)
            assertEquals(1, f.port.deleted.size)
            val receipts = first.snapshot.items.indices.filter { first.snapshot.items[it].phase == BatchItemPhase.PUBLISHED }
                .associateWith { f.receipt(it) }
            val outputs = f.port.bytes.mapValues { it.value.copyOf() }
            f.assertSourcesAndCandidates()

            val restarted = f.coordinator()
            assertTrue(ExportDraftJournal.same(f.draft, restarted.restoreDraft(f.token)!!))
            assertTrue(restarted.start(f.draft, f.token) is BatchRunResult.Existing)
            assertEquals(37, f.port.creates)
            val remaining = restarted.retryableIds(f.token)
            assertEquals(64, remaining.size)
            val done = restarted.retry(f.token, remaining) as BatchRunResult.Finished
            assertTrue(done.snapshot.items.all { it.phase == BatchItemPhase.PUBLISHED })
            assertEquals(101, f.port.creates)
            assertEquals(100, f.port.visible.size)
            receipts.forEach { (index, bytes) -> assertArrayEquals(bytes, f.receipt(index)) }
            outputs.forEach { (uri, bytes) -> assertArrayEquals(bytes, f.port.bytes[uri]) }
            assertTrue(restarted.retryableIds(f.token).isEmpty())
            assertTrue(restarted.retry(f.token, emptySet()) is BatchRunResult.Existing)
            assertEquals(101, f.port.creates)
            f.assertPublishedPixels()
            f.assertSourcesAndCandidates()
        } finally { f.root.deleteRecursively() }
    }

    @Test fun hundredItemUncertainPublicationCannotBeRetriedWhileUntouchedItemsCanContinue() {
        val f = Fixture()
        try {
            f.port.uncertainPublishAt = 37
            val first = f.coordinator().start(f.draft, f.token) as BatchRunResult.NeedsReview
            assertEquals(36, first.snapshot!!.items.count { it.phase == BatchItemPhase.PUBLISHED })
            assertEquals(BatchItemPhase.NEEDS_REVIEW, first.snapshot!!.items[36].phase)
            assertEquals(63, first.snapshot!!.items.count { it.phase == BatchItemPhase.QUEUED })
            val uncertainReceipt = f.receipt(36)
            val original = f.port.bytes.mapValues { it.value.copyOf() }
            val restarted = f.coordinator()
            val uncertainId = f.draft.images[36].id
            assertFalse(uncertainId in restarted.retryableIds(f.token))
            assertTrue(restarted.retry(f.token, setOf(uncertainId)) is BatchRunResult.NeedsReview)
            assertEquals(37, f.port.creates)
            assertEquals(63, restarted.retryableIds(f.token).size)
            val done = restarted.retry(f.token, restarted.retryableIds(f.token)) as BatchRunResult.Finished
            assertEquals(99, done.snapshot.items.count { it.phase == BatchItemPhase.PUBLISHED })
            assertEquals(BatchItemPhase.NEEDS_REVIEW, done.snapshot.items[36].phase)
            assertEquals(100, f.port.creates)
            assertEquals(100, f.port.visible.size)
            assertTrue(f.port.deleted.isEmpty())
            assertArrayEquals(uncertainReceipt, f.receipt(36))
            original.forEach { (uri, bytes) -> assertArrayEquals(bytes, f.port.bytes[uri]) }
            assertTrue(restarted.retryableIds(f.token).isEmpty())
            f.assertSourcesAndCandidates()
        } finally { f.root.deleteRecursively() }
    }
}
