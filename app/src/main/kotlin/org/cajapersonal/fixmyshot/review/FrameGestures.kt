package org.cajapersonal.fixmyshot.review

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import org.cajapersonal.fixmyshot.core.*
import kotlin.math.abs

/** Flexible-frame gestures never publish a draft while a pointer remains down. */
@Composable
internal fun Modifier.frameGestures(image: DraftImage?, plan: PreviewPlan?, enabled: Boolean,
    onPreview: (FrameMotion) -> Unit, onFinished: (FrameFocus) -> Unit,
    onTap: (() -> Unit)? = null, onStarted: () -> Unit = {},
    onCancelled: () -> Unit = {}): Modifier {
    val latestPlan by rememberUpdatedState(plan)
    val latestPreview by rememberUpdatedState(onPreview)
    val latestFinish by rememberUpdatedState(onFinished)
    val latestTap by rememberUpdatedState(onTap)
    val latestStart by rememberUpdatedState(onStarted)
    val latestCancel by rememberUpdatedState(onCancelled)
    return pointerInput(image?.id, enabled, plan?.sessionRevision, plan?.itemRevision,
        plan?.settings?.fit, plan?.settings?.frame, plan?.destination) {
        if (!enabled || image == null || plan?.settings?.fit != FitMode.FLEXIBLE) return@pointerInput
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            val current = latestPlan ?: return@awaitEachGesture
            val ratio = current.destination.cropRatio(image.orientedSize, current.settings.size)
            var motion = FrameInteraction.start(image.orientedSize, ratio, current.settings.frame)
            var panTotal = Offset.Zero
            var zoomTotal = 1f
            var moved = false
            var changed = false
            var committed = false
            var cancelled = false
            latestStart()
            try {
                do {
                    val event = awaitPointerEvent()
                    if (event.changes.any { it.isConsumed }) { cancelled = true; break }
                    val pan = event.calculatePan()
                    val scale = event.calculateZoom()
                    panTotal += pan
                    zoomTotal *= scale
                    if (!moved) moved = panTotal.getDistance() > viewConfiguration.touchSlop ||
                        abs(zoomTotal - 1f) * event.calculateCentroidSize(false) > viewConfiguration.touchSlop
                    if (moved && (pan != Offset.Zero || scale != 1f)) {
                        val center = event.calculateCentroid(false)
                        try {
                            motion = FrameInteraction.move(image.orientedSize, ratio, motion,
                                size.width.toDouble(), size.height.toDouble(),
                                center.x.toDouble(), center.y.toDouble(),
                                pan.x.toDouble(), pan.y.toDouble(), scale.toDouble())
                            latestPreview(motion)
                            changed = true
                        } catch (_: IllegalArgumentException) { /* Keep the last valid gesture frame. */ }
                    }
                    if (moved) event.changes.forEach { if (it.positionChanged()) it.consume() }
                } while (event.changes.any { it.pressed })
                if (!cancelled) {
                    committed = true
                    if (changed) latestFinish(motion.focus) else latestTap?.invoke()
                }
            } finally { if (!committed) latestCancel() }
        }
    }
}
