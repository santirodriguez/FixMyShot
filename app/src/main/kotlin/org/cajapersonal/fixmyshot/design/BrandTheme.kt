package org.cajapersonal.fixmyshot.design

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Night = darkColorScheme(
    primary = Color(0xFF65DDF6), onPrimary = Color(0xFF002F3B),
    primaryContainer = Color(0xFF123D50), onPrimaryContainer = Color(0xFFB9EFFF),
    secondary = Color(0xFFF1A1CA), onSecondary = Color(0xFF471D34),
    secondaryContainer = Color(0xFF432B40), onSecondaryContainer = Color(0xFFFFD8EA),
    tertiary = Color(0xFFFFB873), onTertiary = Color(0xFF492700),
    background = Color(0xFF0A101E), onBackground = Color(0xFFE5ECF7),
    surface = Color(0xFF101929), onSurface = Color(0xFFE5ECF7),
    surfaceVariant = Color(0xFF1B293E), onSurfaceVariant = Color(0xFFB8C7DA),
    surfaceContainer = Color(0xFF152236), surfaceContainerLow = Color(0xFF101B2C),
    surfaceContainerHigh = Color(0xFF21314A), outline = Color(0xFF73869D),
    outlineVariant = Color(0xFF33455C), error = Color(0xFFFFB4AB),
)
private val Day = lightColorScheme(
    primary = Color(0xFF00647B), onPrimary = Color.White,
    primaryContainer = Color(0xFFC9F1FD), onPrimaryContainer = Color(0xFF003542),
    secondary = Color(0xFF8C3F69), onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFD8EA), onSecondaryContainer = Color(0xFF4F1938),
    tertiary = Color(0xFF835116), onTertiary = Color.White,
    background = Color(0xFFF5F8FF), onBackground = Color(0xFF142136),
    surface = Color(0xFFF9FAFF), onSurface = Color(0xFF142136),
    surfaceVariant = Color(0xFFE3EBF5), onSurfaceVariant = Color(0xFF42546A),
    surfaceContainer = Color(0xFFEBF1FA), surfaceContainerLow = Color(0xFFF1F5FC),
    surfaceContainerHigh = Color(0xFFE1EAF6), outline = Color(0xFF63768E),
    outlineVariant = Color(0xFFBECCDE),
)

/** Local branding never tints or changes exported pixels. */
@Composable
internal fun FixMyShotTheme(dark: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) Night else Day,
        shapes = Shapes(small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(18.dp),
            large = RoundedCornerShape(24.dp), extraLarge = RoundedCornerShape(28.dp)),
        typography = Typography(
            headlineLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold,
                fontSize = 30.sp, lineHeight = 36.sp, letterSpacing = (-.5).sp),
            titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold,
                fontSize = 22.sp, lineHeight = 28.sp),
            titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp, lineHeight = 22.sp),
        ), content = content)
}
