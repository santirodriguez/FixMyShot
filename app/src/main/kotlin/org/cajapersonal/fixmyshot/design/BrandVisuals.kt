package org.cajapersonal.fixmyshot.design

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Small decorative artwork for verified success only; never enters the exported bitmap. */
@Composable
internal fun SavedSuccessArtwork(modifier: Modifier = Modifier.size(164.dp)) {
    Canvas(modifier) {
        val unit = size.minDimension / 100f
        val rounded = CornerRadius(8f * unit)
        rotate(-10f, pivot = Offset(50f * unit, 50f * unit)) {
            drawRoundRect(Color(0xFFD8E7FA), Offset(16f * unit, 23f * unit),
                Size(58f * unit, 62f * unit), rounded)
            drawRoundRect(Color(0xFF729BC3), Offset(16f * unit, 23f * unit),
                Size(58f * unit, 62f * unit), rounded, style = Stroke(1.5f * unit))
        }
        rotate(5f, pivot = Offset(50f * unit, 50f * unit)) {
            drawRoundRect(Color.White, Offset(27f * unit, 12f * unit),
                Size(58f * unit, 65f * unit), rounded)
            drawRoundRect(Color(0xFF7E9CB8), Offset(27f * unit, 12f * unit),
                Size(58f * unit, 65f * unit), rounded, style = Stroke(1.5f * unit))
            drawRoundRect(Color(0xFFBCE9FC), Offset(34f * unit, 20f * unit),
                Size(44f * unit, 38f * unit), CornerRadius(4f * unit))
            val hills = Path().apply {
                moveTo(34f * unit, 55f * unit)
                lineTo(48f * unit, 36f * unit)
                lineTo(57f * unit, 45f * unit)
                lineTo(66f * unit, 32f * unit)
                lineTo(78f * unit, 55f * unit)
                close()
            }
            drawPath(hills, Color(0xFF4294B5))
            drawCircle(Color(0xFFFFC66D), 5f * unit, Offset(65f * unit, 31f * unit))
        }
        drawCircle(Color(0xFFDCF8E9), 20f * unit, Offset(74f * unit, 75f * unit))
        drawCircle(Color(0xFF2DBA82), 20f * unit, Offset(74f * unit, 75f * unit),
            style = Stroke(2f * unit))
        val check = Path().apply {
            moveTo(64f * unit, 75f * unit)
            lineTo(71f * unit, 82f * unit)
            lineTo(84f * unit, 67f * unit)
        }
        drawPath(check, Color(0xFF13845A),
            style = Stroke(width = 4f * unit, cap = StrokeCap.Round))
        drawLine(Color(0xFFE9B75C), Offset(14f * unit, 42f * unit),
            Offset(24f * unit, 42f * unit), 2.5f * unit, StrokeCap.Round)
        drawLine(Color(0xFFE9B75C), Offset(19f * unit, 37f * unit),
            Offset(19f * unit, 47f * unit), 2.5f * unit, StrokeCap.Round)
    }
}

/** Original privacy emblem: an app UI ornament, not a security certification. */
@Composable
internal fun PrivacyShield(modifier: Modifier = Modifier.size(24.dp)) {
    Canvas(modifier) {
        val unit = size.minDimension / 24f
        val shield = Path().apply {
            moveTo(12f * unit, 2f * unit)
            lineTo(21f * unit, 6f * unit)
            lineTo(21f * unit, 12f * unit)
            cubicTo(21f * unit, 18f * unit, 17f * unit, 21f * unit, 12f * unit, 23f * unit)
            cubicTo(7f * unit, 21f * unit, 3f * unit, 18f * unit, 3f * unit, 12f * unit)
            lineTo(3f * unit, 6f * unit)
            close()
        }
        drawPath(shield, Color(0xFFC9F3FA))
        drawPath(shield, Color(0xFF00778C), style = Stroke(1.5f * unit))
        val check = Path().apply {
            moveTo(7f * unit, 12f * unit)
            lineTo(11f * unit, 16f * unit)
            lineTo(17f * unit, 9f * unit)
        }
        drawPath(check, Color(0xFF00647B), style = Stroke(2f * unit, cap = StrokeCap.Round))
    }
}

/**
 * Small offline identifiers for descriptive destination names, not official third-party
 * trademark assets or claims of integration/endorsement. Platform text remains primary.
 * No third-party brand artwork is distributed without separate rights review.
 */
@Composable
internal fun DestinationPlatformMark(platform: String, modifier: Modifier = Modifier.size(32.dp)) {
    val background = when (platform) {
        "WhatsApp" -> Color(0xFF16865B)
        "Instagram" -> Color(0xFFA33F89)
        "Facebook" -> Color(0xFF215FAE)
        "TikTok" -> Color(0xFF222A34)
        "Google Play" -> Color(0xFF257D9C)
        else -> Color(0xFF52667E)
    }
    Box(modifier.background(background, RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center) {
        when (platform) {
            "Facebook" -> Text("F", color = Color.White, fontSize = 18.sp,
                fontWeight = FontWeight.Bold)
            else -> Canvas(Modifier.size(22.dp)) {
                val u = size.minDimension / 24f
                when (platform) {
                    "WhatsApp" -> {
                        val bubble = Path().apply {
                            moveTo(5f*u, 19f*u); lineTo(6.5f*u, 15f*u)
                            cubicTo(2f*u, 10f*u, 6f*u, 4f*u, 12f*u, 4f*u)
                            cubicTo(19f*u, 4f*u, 22f*u, 10f*u, 18f*u, 16f*u)
                            cubicTo(15f*u, 20f*u, 10f*u, 21f*u, 7f*u, 18f*u)
                            close()
                        }
                        drawPath(bubble, Color.White, style = Stroke(2f*u, cap = StrokeCap.Round))
                        drawCircle(Color.White, 1.3f*u, Offset(9f*u, 11f*u))
                        drawCircle(Color.White, 1.3f*u, Offset(14f*u, 11f*u))
                    }
                    "Instagram" -> {
                        drawRoundRect(Color.White, Offset(3f*u, 3f*u), Size(18f*u, 18f*u),
                            CornerRadius(5f*u), style = Stroke(2f*u))
                        drawCircle(Color.White, 4f*u, Offset(12f*u, 12f*u), style = Stroke(2f*u))
                        drawCircle(Color.White, 1.5f*u, Offset(17.5f*u, 6.5f*u))
                    }
                    "TikTok" -> {
                        val note = Path().apply {
                            moveTo(13f*u, 5f*u); lineTo(13f*u, 17f*u)
                            cubicTo(10f*u, 21f*u, 5f*u, 18f*u, 7f*u, 15f*u)
                            cubicTo(9f*u, 13f*u, 11f*u, 14f*u, 13f*u, 14f*u)
                            moveTo(13f*u, 5f*u)
                            cubicTo(15f*u, 8f*u, 17f*u, 8f*u, 20f*u, 8f*u)
                        }
                        drawPath(note, Color.White, style = Stroke(2.3f*u, cap = StrokeCap.Round))
                    }
                    "Google Play" -> {
                        val triangle = Path().apply {
                            moveTo(6f*u, 3f*u); lineTo(20f*u, 12f*u)
                            lineTo(6f*u, 21f*u); close()
                        }
                        drawPath(triangle, Color.White)
                    }
                    else -> {
                        drawCircle(Color.White, 7f*u, Offset(12f*u, 12f*u),
                            style = Stroke(1.8f*u))
                        drawLine(Color.White, Offset(6f*u, 12f*u),
                            Offset(18f*u, 12f*u), 1.8f*u)
                    }
                }
            }
        }
    }
}
