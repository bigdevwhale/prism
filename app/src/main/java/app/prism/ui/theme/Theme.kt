package app.prism.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.prism.R
import app.prism.data.ThemeMode

/** White light on graphite, split into a spectrum; by day, ink on warm paper with deeper colours. */
@Immutable
data class Palette(
    val bg: Color,
    /** A faint glow at the top of the screen. */
    val bgTop: Color,
    val surface: Color,
    val surface2: Color,
    val line: Color,
    val ink: Color,
    val muted: Color,
    val faint: Color,
    /** The white beam: primary buttons, the core. */
    val accent: Color,
    val accentSoft: Color,
    val onAccent: Color,
    val good: Color,
    val danger: Color,
    /** Shadows: a dim smoky violet. */
    val shadow: Color,
    /** The spectrum roles and spheres are coloured from. */
    val hues: List<Color>,
    val dark: Boolean,
) {
    fun hue(i: Int): Color = hues[Math.floorMod(i, hues.size)]
}

val NightPalette = Palette(
    bg = Color(0xFF0D0E13),
    bgTop = Color(0xFF1A1A2B),
    surface = Color(0xFF15161E),
    surface2 = Color(0xFF1C1D27),
    line = Color(0xFF2A2B39),
    ink = Color(0xFFEFEDF4),
    muted = Color(0xFF9C9AB0),
    faint = Color(0xFF66657B),
    accent = Color(0xFFF3EFE4),
    accentSoft = Color(0xFF2A2933),
    onAccent = Color(0xFF111117),
    good = Color(0xFF7FE0A8),
    danger = Color(0xFFFF7B7B),
    shadow = Color(0xFF8C7AA8),
    hues = listOf(
        Color(0xFFFF6F6F), // red
        Color(0xFFFF9A5C), // orange
        Color(0xFFFFC754), // amber
        Color(0xFFF0E070), // yellow
        Color(0xFFB4E26E), // lime
        Color(0xFF6EDB9C), // green
        Color(0xFF52D2C4), // teal
        Color(0xFF5DC0F2), // cyan
        Color(0xFF7090FF), // blue
        Color(0xFF9C7EFF), // indigo
        Color(0xFFC982FF), // violet
        Color(0xFFFF82C6), // magenta
    ),
    dark = true,
)

val PaperPalette = Palette(
    bg = Color(0xFFF6F4EF),
    bgTop = Color(0xFFEDEAF6),
    surface = Color(0xFFFFFFFF),
    surface2 = Color(0xFFF9F8F4),
    line = Color(0xFFE4E1DA),
    ink = Color(0xFF1C1B23),
    muted = Color(0xFF615F70),
    faint = Color(0xFF9E9CAA),
    accent = Color(0xFF1C1B23),
    accentSoft = Color(0xFFEBE8E1),
    onAccent = Color(0xFFFFFFFF),
    good = Color(0xFF2E9A62),
    danger = Color(0xFFD5443F),
    shadow = Color(0xFF6E5E8A),
    hues = listOf(
        Color(0xFFD9443F),
        Color(0xFFDB6A27),
        Color(0xFFC48A0E),
        Color(0xFFA89A12),
        Color(0xFF6F9E22),
        Color(0xFF2E9A62),
        Color(0xFF1E9488),
        Color(0xFF1D86B8),
        Color(0xFF3D5FD6),
        Color(0xFF6A4FD4),
        Color(0xFF9545C9),
        Color(0xFFC9468E),
    ),
    dark = false,
)

val LocalPalette = staticCompositionLocalOf { NightPalette }

/** Body text. */
val Inter = FontFamily(
    Font(R.font.inter_light, FontWeight(300)),
    Font(R.font.inter_regular, FontWeight(400)),
    Font(R.font.inter_medium, FontWeight(500)),
    Font(R.font.inter_semibold, FontWeight(600)),
)

/** Display and mottos: a book serif, italic for the lines that switch a role on. */
val Lora = FontFamily(
    Font(R.font.lora_regular, FontWeight(400)),
    Font(R.font.lora_medium, FontWeight(500)),
    Font(R.font.lora_italic, FontWeight(400), FontStyle.Italic),
)

object U {
    val c: Palette @Composable get() = LocalPalette.current

    val RXl = RoundedCornerShape(28.dp)
    val RLg = RoundedCornerShape(22.dp)
    val RMd = RoundedCornerShape(18.dp)
    val RSm = RoundedCornerShape(12.dp)
}

object UType {
    val Display: TextStyle @Composable get() = TextStyle(fontFamily = Lora, fontWeight = FontWeight(500), fontSize = 30.sp, lineHeight = 38.sp, color = U.c.ink)
    val Title: TextStyle @Composable get() = TextStyle(fontFamily = Lora, fontWeight = FontWeight(500), fontSize = 23.sp, lineHeight = 30.sp, color = U.c.ink)
    val Heading: TextStyle @Composable get() = TextStyle(fontFamily = Lora, fontWeight = FontWeight(500), fontSize = 18.sp, lineHeight = 24.sp, color = U.c.ink)
    val Motto: TextStyle @Composable get() = TextStyle(fontFamily = Lora, fontStyle = FontStyle.Italic, fontSize = 19.sp, lineHeight = 27.sp, color = U.c.ink)
    val Sub: TextStyle @Composable get() = TextStyle(fontFamily = Inter, fontWeight = FontWeight(400), fontSize = 14.5.sp, lineHeight = 21.sp, color = U.c.muted)
    val Eyebrow: TextStyle @Composable get() = TextStyle(fontFamily = Inter, fontWeight = FontWeight(600), fontSize = 11.sp, letterSpacing = 0.12.em, color = U.c.faint)
    val Strong: TextStyle @Composable get() = TextStyle(fontFamily = Inter, fontWeight = FontWeight(600), fontSize = 15.5.sp, lineHeight = 21.sp, color = U.c.ink)
    val Body: TextStyle @Composable get() = TextStyle(fontFamily = Inter, fontWeight = FontWeight(400), fontSize = 15.sp, lineHeight = 22.sp, color = U.c.ink)
    val Muted: TextStyle @Composable get() = TextStyle(fontFamily = Inter, fontWeight = FontWeight(400), fontSize = 13.sp, lineHeight = 18.sp, color = U.c.muted)
    val Small: TextStyle @Composable get() = TextStyle(fontFamily = Inter, fontWeight = FontWeight(500), fontSize = 12.sp, color = U.c.faint)
    val Number: TextStyle @Composable get() = TextStyle(fontFamily = Lora, fontWeight = FontWeight(500), fontSize = 24.sp, color = U.c.ink, fontFeatureSettings = "lnum")
    val Big: TextStyle @Composable get() = TextStyle(fontFamily = Lora, fontWeight = FontWeight(400), fontSize = 46.sp, color = U.c.ink, fontFeatureSettings = "lnum")
    val Button: TextStyle @Composable get() = TextStyle(fontFamily = Inter, fontWeight = FontWeight(600), fontSize = 15.sp)
}

@Composable
fun isDark(mode: ThemeMode): Boolean = when (mode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

@Composable
fun PrismTheme(dark: Boolean, content: @Composable () -> Unit) {
    val p = if (dark) NightPalette else PaperPalette
    val scheme = if (dark) darkColorScheme() else lightColorScheme()
    CompositionLocalProvider(LocalPalette provides p) {
        MaterialTheme(
            colorScheme = scheme.copy(
                primary = p.accent,
                onPrimary = p.onAccent,
                primaryContainer = p.accentSoft,
                onPrimaryContainer = p.ink,
                secondaryContainer = p.accentSoft,
                onSecondaryContainer = p.ink,
                background = p.bg,
                surface = p.surface,
                surfaceContainer = p.surface,
                surfaceContainerHigh = p.surface,
                surfaceContainerHighest = p.surface2,
                surfaceContainerLow = p.surface2,
                onBackground = p.ink,
                onSurface = p.ink,
                onSurfaceVariant = p.muted,
                outline = p.line,
                outlineVariant = p.line,
                error = p.danger,
            ),
            typography = MaterialTheme.typography.copy(
                bodyLarge = UType.Body,
                bodyMedium = UType.Body.copy(fontSize = 14.sp),
                labelLarge = UType.Button,
            ),
            content = content,
        )
    }
}
