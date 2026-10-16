package app.prism.ui.practices

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.prism.R
import app.prism.container
import app.prism.data.Role
import app.prism.data.Scene
import app.prism.ui.Go
import app.prism.ui.WData
import app.prism.ui.components.Bar
import app.prism.ui.components.PrimaryButton
import app.prism.ui.components.RoleRow
import app.prism.ui.components.SoftButton
import app.prism.ui.components.motionOn
import app.prism.ui.components.roleColor
import app.prism.ui.g
import app.prism.ui.gs
import app.prism.ui.roleName
import app.prism.ui.theme.U
import app.prism.ui.theme.UType
import kotlinx.coroutines.delay

private const val STEPS = 5

@Composable
fun SosScreen(data: WData, go: Go) {
    val w = data.world
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var step by rememberSaveable { mutableIntStateOf(0) }
    var exitLine by rememberSaveable { mutableStateOf("") }
    var chosen by remember { mutableStateOf<Role?>(null) }

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { go.back() }) { Icon(Icons.Rounded.Close, gs(R.string.close), tint = U.c.muted) }
            Text(gs(R.string.pr_sos), style = UType.Heading, modifier = Modifier.weight(1f))
            Text("${step + 1}/$STEPS", style = UType.Small, modifier = Modifier.padding(end = 16.dp))
        }
        Mountain(step, Modifier.fillMaxWidth().height(130.dp))
        Bar((step + 1f) / STEPS, Modifier.padding(horizontal = 24.dp), color = U.c.accent.copy(alpha = 0.6f), height = 3.dp)
        AnimatedContent(step, transitionSpec = { fadeIn() togetherWith fadeOut() }, modifier = Modifier.weight(1f), label = "sos") { s ->
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                when (s) {
                    0 -> SosText(gs(R.string.sos_stop_title), gs(R.string.sos_stop_text))
                    1 -> Breathing478(data.settings.haptics, onDone = { step = 2 }) { haptic.tick(it) }
                    2 -> {
                        SosText(gs(R.string.sos_ground_title), gs(R.string.sos_ground_text))
                        Spacer(Modifier.height(18.dp))
                        stringArrayResource(R.array.sos_ground_steps).forEach { line ->
                            Text("·  " + g(line), style = UType.Body, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp))
                        }
                    }
                    3 -> {
                        SosText(gs(R.string.sos_exit_title), gs(R.string.sos_exit_text))
                        Spacer(Modifier.height(18.dp))
                        stringArrayResource(R.array.sos_exit_lines).forEach { line ->
                            val l = g(line)
                            val on = exitLine == l
                            Text(
                                l,
                                style = UType.Body.copy(color = if (on) U.c.ink else U.c.muted),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 5.dp)
                                    .clip(U.RMd)
                                    .background(if (on) U.c.accentSoft else U.c.surface.copy(alpha = 0.6f))
                                    .border(1.dp, if (on) U.c.accent.copy(alpha = 0.5f) else U.c.line, U.RMd)
                                    .clickable { exitLine = l }
                                    .padding(14.dp),
                            )
                        }
                    }
                    else -> {
                        val pick = chosen
                        if (pick == null) {
                            SosText(gs(R.string.sos_return_title), gs(R.string.sos_return_text))
                            Spacer(Modifier.height(12.dp))
                            val list = w.roles.sortedByDescending { w.sphere(it.sphereId)?.presetKey == "conflict" }
                            if (list.isEmpty()) Text(gs(R.string.picker_no_roles), style = UType.Sub)
                            list.forEach { r ->
                                RoleRow(r, w, onClick = {
                                    chosen = r
                                    val now = System.currentTimeMillis()
                                    context.container.write {
                                        insertScene(Scene(day = data.day, at = now, roleId = r.id, kind = Scene.SOS, note = exitLine))
                                    }
                                })
                                HorizontalDivider(color = U.c.line)
                            }
                        } else {
                            val color = roleColor(w, pick)
                            SosText(gs(R.string.sos_returned_title), gs(R.string.sos_returned_text, roleName(pick)))
                            if (pick.motto.isNotBlank()) {
                                Spacer(Modifier.height(18.dp))
                                Text("«${pick.motto}»", style = UType.Motto.copy(color = color, textAlign = TextAlign.Center))
                            }
                            if (exitLine.isNotBlank()) {
                                Spacer(Modifier.height(18.dp))
                                Text(gs(R.string.sos_your_line), style = UType.Eyebrow)
                                Spacer(Modifier.height(6.dp))
                                Text(exitLine, style = UType.Body.copy(textAlign = TextAlign.Center))
                            }
                            Spacer(Modifier.height(28.dp))
                            PrimaryButton(gs(R.string.sos_threshold, roleName(pick)), { go.back(); go.threshold(pick.id) }, Modifier.fillMaxWidth(), color = color)
                            Spacer(Modifier.height(10.dp))
                            SoftButton(gs(R.string.done), { go.back() }, Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        }
        if (step < STEPS - 1) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (step > 0) SoftButton(gs(R.string.back), { step-- }, Modifier.weight(1f), color = U.c.muted)
                PrimaryButton(gs(R.string.next), { step++ }, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SosText(title: String, text: String) {
    Text(title, style = UType.Title.copy(textAlign = TextAlign.Center))
    Spacer(Modifier.height(12.dp))
    Text(text, style = UType.Sub.copy(textAlign = TextAlign.Center))
}

/** Three slow cycles of 4-7-8: breathe in for four, hold for seven, out for eight. */
@Composable
private fun Breathing478(haptics: Boolean, onDone: () -> Unit, tick: (Boolean) -> Unit) {
    val c = U.c
    var t by remember { mutableFloatStateOf(0f) }
    val cycle = 19f
    val total = cycle * 3
    LaunchedEffect(Unit) {
        val start = System.currentTimeMillis()
        var lastPhase = -1
        while (t < total) {
            t = (System.currentTimeMillis() - start) / 1000f
            val ph = phaseOf(t % cycle) + 3 * (t / cycle).toInt()
            if (ph != lastPhase) { tick(haptics); lastPhase = ph }
            delay(50)
        }
        onDone()
    }
    val inCycle = t % cycle
    val phase = phaseOf(inCycle)
    val k = when (phase) {
        0 -> inCycle / 4f
        1 -> 1f
        else -> 1f - (inCycle - 11f) / 8f
    }
    val scale = if (motionOn()) 0.45f + 0.55f * k.coerceIn(0f, 1f) else 0.8f
    val left = when (phase) {
        0 -> 4 - inCycle
        1 -> 11 - inCycle
        else -> 19 - inCycle
    }.toInt() + 1
    SosText(gs(R.string.sos_breathe_title), gs(R.string.sos_breathe_text))
    Spacer(Modifier.height(24.dp))
    Box(Modifier.size(200.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val r = size.minDimension / 2 * scale
            drawCircle(Brush.radialGradient(listOf(c.accent.copy(alpha = 0.25f), c.accent.copy(alpha = 0.03f)), radius = r.coerceAtLeast(1f)), radius = r)
            drawCircle(c.accent.copy(alpha = 0.5f), radius = r, style = androidx.compose.ui.graphics.drawscope.Stroke(1.5f * density))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(gs(listOf(R.string.sos_in, R.string.sos_hold, R.string.sos_out)[phase]), style = UType.Heading)
            Text(left.toString(), style = UType.Number.copy(color = c.muted))
        }
    }
    Spacer(Modifier.height(16.dp))
    Text(gs(R.string.sos_cycle, (t / cycle).toInt().coerceAtMost(2) + 1), style = UType.Small)
    Spacer(Modifier.width(1.dp))
}

private fun phaseOf(t: Float) = when {
    t < 4f -> 0
    t < 11f -> 1
    else -> 2
}

/** A quiet mountain range; the snow line rises with each step. */
@Composable
private fun Mountain(step: Int, modifier: Modifier) {
    val c = U.c
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val back = Path().apply {
            moveTo(0f, h)
            lineTo(0f, h * 0.7f)
            lineTo(w * 0.18f, h * 0.45f)
            lineTo(w * 0.32f, h * 0.62f)
            lineTo(w * 0.72f, h * 0.3f)
            lineTo(w, h * 0.6f)
            lineTo(w, h)
            close()
        }
        drawPath(back, c.muted.copy(alpha = 0.12f))
        val peak = Offset(w * 0.5f, h * 0.12f)
        val front = Path().apply {
            moveTo(0f, h)
            lineTo(w * 0.12f, h * 0.82f)
            lineTo(peak.x, peak.y)
            lineTo(w * 0.86f, h * 0.78f)
            lineTo(w, h)
            close()
        }
        drawPath(front, Brush.verticalGradient(listOf(c.accent.copy(alpha = 0.28f), c.accent.copy(alpha = 0.04f)), startY = peak.y, endY = h))
        val snow = 0.12f + 0.06f * step
        val snowY = peak.y + (h - peak.y) * snow
        val dxL = (peak.x - w * 0.12f) * snow
        val dxR = (w * 0.86f - peak.x) * snow
        val cap = Path().apply {
            moveTo(peak.x, peak.y)
            lineTo(peak.x + dxR, snowY)
            lineTo(peak.x + dxR * 0.4f, snowY - h * 0.03f)
            lineTo(peak.x, snowY + h * 0.02f)
            lineTo(peak.x - dxL * 0.5f, snowY - h * 0.03f)
            lineTo(peak.x - dxL, snowY)
            close()
        }
        drawPath(cap, c.accent.copy(alpha = 0.7f))
    }
}
