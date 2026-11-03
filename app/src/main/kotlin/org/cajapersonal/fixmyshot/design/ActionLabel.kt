package org.cajapersonal.fixmyshot.design

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

internal enum class ActionMark { INFO, VIEW, SHARE, ADD, EDIT, DISCARD, CLOSE }

/** Decorative local vectors accompany localized labels; no icon font or dependency. */
@Composable
internal fun ActionIcon(mark: ActionMark, description: String? = null) {
    val vector = remember(mark) { ImageVector.Builder(mark.name, 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = null, stroke = androidx.compose.ui.graphics.SolidColor(Color.Black), strokeLineWidth = 1.8f) {
            when (mark) {
                ActionMark.INFO -> { moveTo(12f,3f); curveTo(24f,3f,24f,21f,12f,21f); curveTo(0f,21f,0f,3f,12f,3f); moveTo(12f,10f); lineTo(12f,17f); moveTo(12f,7f); lineTo(12f,8f) }
                ActionMark.VIEW -> { moveTo(2f,12f); curveTo(7f,3f,17f,3f,22f,12f); curveTo(17f,21f,7f,21f,2f,12f); moveTo(12f,9f); curveTo(16f,9f,16f,15f,12f,15f); curveTo(8f,15f,8f,9f,12f,9f) }
                ActionMark.SHARE -> { moveTo(6f,12f); lineTo(18f,5f); moveTo(6f,12f); lineTo(18f,19f); moveTo(3f,10f); lineTo(7f,10f); lineTo(7f,14f); lineTo(3f,14f); close(); moveTo(16f,3f); lineTo(20f,3f); lineTo(20f,7f); lineTo(16f,7f); close(); moveTo(16f,17f); lineTo(20f,17f); lineTo(20f,21f); lineTo(16f,21f); close() }
                ActionMark.ADD -> { moveTo(12f,4f); lineTo(12f,20f); moveTo(4f,12f); lineTo(20f,12f) }
                ActionMark.EDIT -> { moveTo(4f,20f); lineTo(5f,15f); lineTo(17f,3f); lineTo(21f,7f); lineTo(9f,19f); close(); moveTo(14f,6f); lineTo(18f,10f) }
                ActionMark.DISCARD -> { moveTo(4f,6f); lineTo(20f,6f); moveTo(9f,6f); lineTo(9f,3f); lineTo(15f,3f); lineTo(15f,6f); moveTo(6f,6f); lineTo(7f,21f); lineTo(17f,21f); lineTo(18f,6f); moveTo(10f,10f); lineTo(10f,17f); moveTo(14f,10f); lineTo(14f,17f) }
                ActionMark.CLOSE -> { moveTo(6f,6f); lineTo(18f,18f); moveTo(18f,6f); lineTo(6f,18f) }
            }
        }
    }.build() }
    Icon(vector, description, Modifier.size(20.dp))
}

@Composable
internal fun ActionLabel(mark: ActionMark, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        ActionIcon(mark)
        Text(label)
    }
}
