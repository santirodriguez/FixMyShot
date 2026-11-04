package org.cajapersonal.fixmyshot.review

import java.io.Closeable
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/** Only replaceable previews: one running task and the latest pending task, never export work.
 * Pending tasks must not own decoded bitmaps or completion promises before they start.
 */
internal class LatestPreviewWorker : Closeable {
    private val executor = ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
        ArrayBlockingQueue<Runnable>(1), ThreadPoolExecutor.DiscardOldestPolicy())

    @Synchronized fun execute(work: () -> Unit) { executor.execute { work() } }

    @Synchronized fun cancelPending() { executor.queue.clear() }

    /** Interrupt is best effort; stale-result checks remain necessary around native decoding. */
    @Synchronized override fun close() { executor.shutdownNow() }
}
