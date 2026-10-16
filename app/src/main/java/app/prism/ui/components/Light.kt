package app.prism.ui.components

import android.animation.ValueAnimator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.sp
import app.prism.ui.theme.Inter
import app.prism.ui.theme.U
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** False when the user turned animations off in system settings. */
fun motionOn(): Boolean = ValueAnimator.areAnimatorsEnabled()

/** The background of every screen: graphite with a faint glow at the top and a thin diagonal beam. */
@Composable
fun Backdrop(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val c = U.c
    Box(modifier.background(Brush.verticalGradient(0f to c.bgTop, 0.45f to c.bg, 1f to c.bg))) {
        Canvas(Modifier.fillMaxSize()) {
            val beam = Path().apply {
                moveTo(0f, size.height * 0.08f)
                lineTo(size.width, size.height * 0.02f)
                lineTo(size.width, size.height * 0.07f)
                close()
            }
            drawPath(beam, Brush.horizontalGradient(listOf(c.accent.copy(alpha = 0f), c.accent.copy(alpha = if (c.dark) 0.05f else 0.04f))))
        }
        content()
    }
}

/** One ray leaving the prism. [weight] sets its width, [brightness] (0..1) its glow. */
class Beam(val color: Color, val weight: Float, val brightness: Float, val label: String = "")

/**
 * The prism: white light from the left, split into one coloured ray per role.
 * Rays fan out from the prism's right face; their order follows [beams].
 */
@Composable
fun PrismView(beams: List<Beam>, modifier: Modifier = Modifier, labels: Boolean = true, animate: Boolean = true) {
    val c = U.c
    val measurer = rememberTextMeasurer()
    val progress = remember { Animatable(if (animate && motionOn()) 0f else 1f) }
    LaunchedEffect(Unit) { progress.animateTo(1f, tween(1400, easing = FastOutSlowInEasing)) }
    val labelStyle = TextStyle(fontFamily = Inter, fontSize = 11.sp, color = c.muted)
    Canvas(modifier) {
        val p = progress.value
        val h = size.height
        val w = size.width
        // The prism: an equilateral-ish triangle pointing up.
        val cx = w * 0.34f
        val side = min(h * 0.62f, w * 0.3f)
        val top = Offset(cx, h * 0.5f - side * 0.55f)
        val left = Offset(cx - side / 2, h * 0.5f + side * 0.45f)
        val right = Offset(cx + side / 2, h * 0.5f + side * 0.45f)
        // Light enters the left face, a third of the way up.
        val entry = lerp(left, top, 0.45f)
        val exit = lerp(right, top, 0.5f)
        val beamIn = Offset(0f, entry.y + (h * 0.12f))
        drawLine(
            Brush.horizontalGradient(listOf(c.accent.copy(alpha = 0f), c.accent.copy(alpha = 0.9f)), startX = 0f, endX = entry.x),
            beamIn, entry, strokeWidth = 3.dp(this), cap = androidx.compose.ui.graphics.StrokeCap.Round,
        )
        // Inside the glass: a soft white wedge from entry to the exit face.
        drawPath(
            Path().apply {
                moveTo(entry.x, entry.y)
                lineTo(exit.x, exit.y - side * 0.12f)
                lineTo(exit.x, exit.y + side * 0.12f)
                close()
            },
            c.accent.copy(alpha = 0.18f),
        )
        // Rays.
        if (beams.isNotEmpty()) {
            val total = beams.sumOf { it.weight.toDouble() }.toFloat().coerceAtLeast(0.001f)
            val spanTop = h * 0.04f
            val spanBottom = h * 0.96f
            val endX = w * (0.34f + 0.66f * p)
            val gap = (spanBottom - spanTop) * 0.12f / max(1, beams.size)
            val usable = (spanBottom - spanTop) - gap * (beams.size - 1)
            var y = spanTop
            val faceTop = exit.y - side * 0.12f
            val faceBottom = exit.y + side * 0.12f
            var fy = faceTop
            beams.forEach { b ->
                val share = b.weight / total
                val band = usable * share
                val fband = (faceBottom - faceTop) * share
                val alpha = (0.25f + 0.75f * b.brightness) * p
                val ray = Path().apply {
                    moveTo(exit.x, fy)
                    lineTo(endX, lerpF(fy, y, p))
                    lineTo(endX, lerpF(fy + fband, y + band, p))
                    lineTo(exit.x, fy + fband)
                    close()
                }
                drawPath(ray, Brush.horizontalGradient(listOf(b.color.copy(alpha = alpha * 0.55f), b.color.copy(alpha = alpha)), startX = exit.x, endX = w))
                if (labels && b.label.isNotEmpty() && p > 0.9f && band > 11.sp.toPx()) {
                    val layout = measurer.measure(
                        b.label, labelStyle.copy(color = c.ink.copy(alpha = 0.85f)), maxLines = 1, overflow = TextOverflow.Ellipsis,
                        constraints = Constraints(maxWidth = (w * 0.3f).toInt()),
                    )
                    drawText(layout, topLeft = Offset(w - layout.size.width - 6f, y + band / 2 - layout.size.height / 2))
                }
                y += band + gap
                fy += fband
            }
        }
        // The glass itself, drawn last so its edges stay crisp over the light.
        val tri = Path().apply { moveTo(top.x, top.y); lineTo(right.x, right.y); lineTo(left.x, left.y); close() }
        drawPath(tri, Brush.verticalGradient(listOf(c.accent.copy(alpha = 0.10f), c.accent.copy(alpha = 0.03f)), startY = top.y, endY = left.y))
        drawPath(tri, c.accent.copy(alpha = 0.75f), style = Stroke(1.5f.dp(this), join = StrokeJoin.Round))
    }
}

/** A small static prism for the logo, empty states and buttons. */
@Composable
fun PrismGlyph(modifier: Modifier = Modifier, dim: Boolean = false) {
    val c = U.c
    val hues = c.hues
    Canvas(modifier) { drawGlyph(c.accent, hues, if (dim) 0.35f else 1f) }
}

private fun DrawScope.drawGlyph(glass: Color, hues: List<Color>, strength: Float) {
    val h = size.height
    val w = size.width
    val side = min(h * 0.9f, w * 0.42f)
    val cx = w * 0.42f
    val top = Offset(cx, h / 2 - side * 0.5f)
    val left = Offset(cx - side / 2, h / 2 + side * 0.45f)
    val right = Offset(cx + side / 2, h / 2 + side * 0.45f)
    val entry = lerp(left, top, 0.45f)
    val exit = lerp(right, top, 0.5f)
    drawLine(glass.copy(alpha = 0.8f * strength), Offset(0f, entry.y + h * 0.1f), entry, strokeWidth = 2f.dp(this))
    val picks = listOf(0, 2, 5, 8, 10)
    picks.forEachIndexed { i, hue ->
        val y0 = h * (0.12f + 0.19f * i)
        drawPath(
            Path().apply { moveTo(exit.x, exit.y - 2f); lineTo(w, y0); lineTo(w, y0 + h * 0.12f); lineTo(exit.x, exit.y + 2f); close() },
            hues[hue].copy(alpha = 0.75f * strength),
        )
    }
    val tri = Path().apply { moveTo(top.x, top.y); lineTo(right.x, right.y); lineTo(left.x, left.y); close() }
    drawPath(tri, glass.copy(alpha = 0.08f * strength))
    drawPath(tri, glass.copy(alpha = 0.8f * strength), style = Stroke(1.5f.dp(this), join = StrokeJoin.Round))
}

/**
 * A role's gem: a hexagonal facet in its colour. [cut] 0..3 adds inner facets as the role is polished.
 */
@Composable
fun Facet(color: Color, modifier: Modifier = Modifier, cut: Int = 0, shadow: Boolean = false) {
    val c = U.c
    Canvas(modifier) {
        val r = min(size.width, size.height) / 2 * 0.92f
        val center = Offset(size.width / 2, size.height / 2)
        val pts = (0 until 6).map { i ->
            val a = PI / 6 + i * PI / 3
            Offset(center.x + r * cos(a).toFloat(), center.y + r * sin(a).toFloat())
        }
        val hex = Path().apply { moveTo(pts[0].x, pts[0].y); pts.drop(1).forEach { lineTo(it.x, it.y) }; close() }
        if (shadow) {
            drawPath(hex, c.shadow.copy(alpha = 0.22f))
            drawPath(hex, c.shadow.copy(alpha = 0.7f), style = Stroke(1.4f.dp(this), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(6f, 5f))))
            return@Canvas
        }
        drawPath(hex, Brush.linearGradient(listOf(color.copy(alpha = 0.95f), color.copy(alpha = 0.55f)), start = pts[4], end = pts[1]))
        val line = Color.White.copy(alpha = 0.45f)
        val sw = 1.1f.dp(this)
        if (cut >= 1) {
            // A table: an inner hexagon.
            val inner = pts.map { lerp(center, it, 0.5f) }
            val p = Path().apply { moveTo(inner[0].x, inner[0].y); inner.drop(1).forEach { lineTo(it.x, it.y) }; close() }
            drawPath(p, line, style = Stroke(sw))
            if (cut >= 2) pts.forEachIndexed { i, o -> drawLine(line, inner[i], o, sw) }
            if (cut >= 3) {
                drawPath(p, Color.White.copy(alpha = 0.28f))
                pts.forEachIndexed { i, _ -> drawLine(line, center, inner[i], sw) }
            }
        }
        drawPath(hex, Color.White.copy(alpha = 0.35f), style = Stroke(sw))
    }
}

/** A radar of spheres: how the scenes of a stretch of time spread over life. Values are 0..1. */
@Composable
fun Radar(values: List<Pair<Color, Float>>, modifier: Modifier = Modifier) {
    val c = U.c
    Canvas(modifier) {
        val n = values.size
        if (n < 3) return@Canvas
        val r = min(size.width, size.height) / 2 * 0.9f
        val center = Offset(size.width / 2, size.height / 2)
        fun at(i: Int, k: Float): Offset {
            val a = -PI / 2 + i * 2 * PI / n
            return Offset(center.x + r * k * cos(a).toFloat(), center.y + r * k * sin(a).toFloat())
        }
        listOf(0.33f, 0.66f, 1f).forEach { k ->
            val ring = Path().apply { moveTo(at(0, k).x, at(0, k).y); (1 until n).forEach { lineTo(at(it, k).x, at(it, k).y) }; close() }
            drawPath(ring, c.line, style = Stroke(1f.dp(this)))
        }
        (0 until n).forEach { drawLine(c.line, center, at(it, 1f), 1f.dp(this)) }
        val shape = Path().apply {
            val v = values.map { it.second.coerceIn(0.04f, 1f) }
            moveTo(at(0, v[0]).x, at(0, v[0]).y)
            (1 until n).forEach { lineTo(at(it, v[it]).x, at(it, v[it]).y) }
            close()
        }
        drawPath(shape, c.accent.copy(alpha = 0.12f))
        drawPath(shape, c.accent.copy(alpha = 0.7f), style = Stroke(1.5f.dp(this), join = StrokeJoin.Round))
        values.forEachIndexed { i, (color, v) -> drawCircle(color, 4f.dp(this), at(i, v.coerceIn(0.04f, 1f))) }
    }
}

private fun lerp(a: Offset, b: Offset, t: Float) = Offset(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t)
private fun lerpF(a: Float, b: Float, t: Float) = a + (b - a) * t
private fun Float.dp(scope: DrawScope): Float = this * scope.density
private fun Int.dp(scope: DrawScope): Float = this * scope.density
