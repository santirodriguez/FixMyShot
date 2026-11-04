package org.cajapersonal.fixmyshot.review

import androidx.compose.foundation.layout.*
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.cajapersonal.fixmyshot.R
import org.cajapersonal.fixmyshot.core.ContainPosition
import org.cajapersonal.fixmyshot.core.PlannedGeometry
import org.cajapersonal.fixmyshot.core.PreviewPlan

/** Only the tool pane scrolls. Short/wide windows put the canvas beside its controls. */
@Composable
internal fun ReviewWorkspace(modifier: Modifier = Modifier, hasImage: Boolean,
    stage: @Composable () -> Unit, controls: @Composable () -> Unit) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        if (!hasImage) controls()
        else if (maxWidth > maxHeight) Row(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f).fillMaxHeight().clipToBounds()) { stage() }
            Box(Modifier.weight(1f).fillMaxHeight().clipToBounds()) { controls() }
        } else Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f).fillMaxWidth().clipToBounds()) { stage() }
            Box(Modifier.weight(1f).fillMaxWidth().clipToBounds()) { controls() }
        }
    }
}

@Composable
internal fun ContainPositionControls(plan: PreviewPlan, enabled: Boolean, onPosition: (ContainPosition) -> Unit) {
    val geometry = (plan.geometry as? PlannedGeometry.Contained)?.value ?: return
    val position = plan.settings.position
    val availableX = geometry.canvas.width - geometry.placement.width
    val availableY = geometry.canvas.height - geometry.placement.height
    fun nudge(dx: Int, dy: Int) = onPosition(ContainPosition(
        if (availableX > 0) (position.x + dx.toDouble() / availableX).coerceIn(0.0, 1.0) else .5,
        if (availableY > 0) (position.y + dy.toDouble() / availableY).coerceIn(0.0, 1.0) else .5))
    Text(stringResource(R.string.margin_details, geometry.placement.left, geometry.rightMargin,
        geometry.placement.top, geometry.bottomMargin))
    if (availableX > 0) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(enabled = enabled && geometry.placement.left > 0, onClick = { nudge(-8, 0) }) {
            Text(stringResource(R.string.preview_position_left))
        }
        OutlinedButton(enabled = enabled && geometry.rightMargin > 0, onClick = { nudge(8, 0) }) {
            Text(stringResource(R.string.preview_position_right))
        }
    }
    if (availableY > 0) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(enabled = enabled && geometry.placement.top > 0, onClick = { nudge(0, -8) }) {
            Text(stringResource(R.string.preview_position_up))
        }
        OutlinedButton(enabled = enabled && geometry.bottomMargin > 0, onClick = { nudge(0, 8) }) {
            Text(stringResource(R.string.preview_position_down))
        }
    }
    OutlinedButton(enabled = enabled && position != ContainPosition(), onClick = { onPosition(ContainPosition()) }) {
        Text(stringResource(R.string.preview_position_center))
    }
}
