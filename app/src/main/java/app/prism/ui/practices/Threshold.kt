package app.prism.ui.practices

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.prism.R
import app.prism.container
import app.prism.data.Role
import app.prism.data.Scene
import app.prism.data.Threshold
import app.prism.domain.lines
import app.prism.ui.Go
import app.prism.ui.WData
import app.prism.ui.components.DaysPicker
import app.prism.ui.components.EmptyState
import app.prism.ui.components.Facet
import app.prism.ui.components.PrimaryButton
import app.prism.ui.components.ProgressRing
import app.prism.ui.components.RolePickerDialog
import app.prism.ui.components.SoftButton
import app.prism.ui.components.TimeDialog
import app.prism.ui.components.UCard
import app.prism.ui.components.UTextField
import app.prism.ui.components.motionOn
import app.prism.ui.components.roleColor
import app.prism.ui.g
import app.prism.ui.gs
import app.prism.ui.hhmm
import app.prism.ui.roleName
import app.prism.ui.theme.U
import app.prism.ui.theme.UType
import app.prism.ui.weekdayShort
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin

/** Shares of the whole crossing: leaving the last scene, the pause, stepping in. */
private const val LEAVE = 0.45f
private const val PAUSE = 0.15f

@Composable
fun ThresholdScreen(roleId: Long, data: WData, go: Go) {
    val w = data.world
    val role = w.role(roleId)
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val hapticsOn = data.settings.haptics
    val total = data.settings.thresholdSeconds.coerceAtLeast(15) * 1000L

    if (role == null) {
        PracticeScaffold(gs(R.string.pr_threshold), { go.back() }) {
            item { EmptyState(gs(R.string.picker_no_roles)) }
        }
        return
    }
    val color = roleColor(w, role)

    var elapsed by remember { mutableFloatStateOf(0f) }
    var done by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        val start = System.currentTimeMillis()
        while (!done) {
            elapsed = ((System.currentTimeMillis() - start).toFloat() / total).coerceAtMost(1f)
            if (elapsed >= 1f) done = true
            delay(50)
        }
    }
    val p = if (done) 1f else elapsed
    val phase = when {
        done -> 3
        p < LEAVE -> 0
        p < LEAVE + PAUSE -> 1
        else -> 2
    }
    LaunchedEffect(phase) {
        if (phase == 3) haptic.thump(hapticsOn) else if (phase > 0) haptic.tick(hapticsOn)
    }
    LaunchedEffect(done) {
        if (done) {
            val now = System.currentTimeMillis()
            context.container.write { insertScene(Scene(day = data.day, at = now, roleId = role.id, kind = Scene.THRESHOLD)) }
        }
    }

    Box(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { go.back() }) { Icon(Icons.Rounded.Close, gs(R.string.close), tint = U.c.muted) }
            Spacer(Modifier.weight(1f))
            ProgressRing(p, Modifier.size(40.dp), stroke = 3.dp, color = color) {
                val left = ((1f - p) * total / 1000f).toInt()
                Text(if (done) "✦" else left.toString(), style = UType.Small.copy(color = U.c.muted))
            }
            Spacer(Modifier.width(12.dp))
        }

        Column(
            Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 64.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            when (phase) {
                0 -> LeavePhase(p / LEAVE, p * total / 1000f)
                1 -> PausePhase()
                else -> StepInPhase(role, color, if (phase == 3) 1f else (p - LEAVE - PAUSE) / (1f - LEAVE - PAUSE))
            }
        }

        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (done) {
                PrimaryButton(gs(R.string.thr_go), { go.back() }, Modifier.fillMaxWidth(), color = color)
            } else {
                Text(
                    gs(R.string.thr_skip),
                    style = UType.Button.copy(color = U.c.faint),
                    modifier = Modifier.clickable { done = true }.padding(12.dp),
                )
            }
        }
    }
}

/** A breathing circle: in for four, a long exhale for six; the old scene goes out with the breath. */
@Composable
private fun LeavePhase(progress: Float, seconds: Float) {
    val c = U.c
    val cycle = 10f
    val t = (seconds % cycle)
    val inhale = t < 4f
    val k = if (motionOn()) (if (inhale) t / 4f else 1f - (t - 4f) / 6f) else 0.6f
    val scale = 0.55f + 0.45f * easeInOut(k)
    val cues = stringArrayResource(R.array.thr_leave_cues)
    val cue = cues[((progress * cues.size).toInt()).coerceIn(0, cues.size - 1)]
    Text(gs(R.string.thr_leave_title), style = UType.Eyebrow)
    Spacer(Modifier.height(28.dp))
    Box(Modifier.size(220.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize().scale(scale)) {
            drawCircle(Brush.radialGradient(listOf(c.accent.copy(alpha = 0.22f), c.accent.copy(alpha = 0.02f))))
            drawCircle(c.accent.copy(alpha = 0.5f), style = androidx.compose.ui.graphics.drawscope.Stroke(1.5f * density))
        }
        Text(gs(if (inhale) R.string.thr_in else R.string.thr_out), style = UType.Heading.copy(color = c.muted))
    }
    Spacer(Modifier.height(28.dp))
    Text(g(cue), style = UType.Sub.copy(textAlign = TextAlign.Center))
}

@Composable
private fun PausePhase() {
    Text(gs(R.string.thr_pause_title), style = UType.Eyebrow)
    Spacer(Modifier.height(28.dp))
    Box(Modifier.size(220.dp), contentAlignment = Alignment.Center) {
        val c = U.c
        Canvas(Modifier.size(10.dp)) { drawCircle(c.accent.copy(alpha = 0.7f)) }
    }
    Spacer(Modifier.height(28.dp))
    Text(gs(R.string.thr_pause_text), style = UType.Sub.copy(textAlign = TextAlign.Center))
}

/** A beam of the role's colour widens; the role, its motto and its qualities appear one by one. */
@Composable
private fun StepInPhase(role: Role, color: Color, progress: Float) {
    val beam by animateFloatAsState(if (motionOn()) progress else 1f, tween(400, easing = FastOutSlowInEasing), label = "beam")
    val qualities = lines(role.qualities)
    val body = role.body
    val anchor = lines(role.anchor)
    Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val wdt = size.width * (0.04f + 0.5f * beam)
            drawRect(
                Brush.verticalGradient(listOf(color.copy(alpha = 0f), color.copy(alpha = 0.55f), color.copy(alpha = 0f))),
                topLeft = Offset(size.width / 2 - wdt / 2, 0f),
                size = androidx.compose.ui.geometry.Size(wdt, size.height),
            )
            val pulse = 0.5f + 0.5f * sin(beam * PI.toFloat() * 3)
            drawCircle(color.copy(alpha = 0.25f + 0.25f * pulse), radius = 18f * density, center = center)
        }
        Facet(color, Modifier.size(44.dp), cut = 1)
    }
    Spacer(Modifier.height(16.dp))
    Text(gs(R.string.thr_step_title), style = UType.Eyebrow)
    Spacer(Modifier.height(10.dp))
    Text(roleName(role), style = UType.Display.copy(textAlign = TextAlign.Center))
    if (role.motto.isNotBlank()) {
        Spacer(Modifier.height(10.dp))
        Text("«${role.motto}»", style = UType.Motto.copy(color = color, textAlign = TextAlign.Center), modifier = Modifier.alpha(fade(progress, 0.1f)))
    }
    Spacer(Modifier.height(18.dp))
    Column(Modifier.verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
        val steps = qualities.size + (if (body.isNotBlank()) 1 else 0) + (if (anchor.isNotEmpty()) 1 else 0)
        var i = 0
        fun at(): Float { val v = 0.2f + 0.7f * i / steps.coerceAtLeast(1); i++; return v }
        if (qualities.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                qualities.forEach { q ->
                    val a = at()
                    AnimatedVisibility(progress >= a, enter = fadeIn(tween(600))) {
                        Text(q, style = UType.Strong.copy(color = U.c.ink))
                    }
                }
            }
        }
        if (body.isNotBlank()) {
            val a = at()
            AnimatedVisibility(progress >= a, enter = fadeIn(tween(700))) {
                Text(body, style = UType.Sub.copy(textAlign = TextAlign.Center), modifier = Modifier.padding(top = 14.dp))
            }
        }
        if (anchor.isNotEmpty()) {
            val a = at()
            AnimatedVisibility(progress >= a, enter = fadeIn(tween(700))) {
                Text(
                    gs(R.string.thr_anchor, anchor.take(2).joinToString(" · ")),
                    style = UType.Muted.copy(textAlign = TextAlign.Center, color = color),
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }
    }
}

private fun fade(p: Float, from: Float) = ((p - from) / 0.15f).coerceIn(0f, 1f)
private fun easeInOut(x: Float) = (0.5f - 0.5f * kotlin.math.cos(PI.toFloat() * x.coerceIn(0f, 1f)))

// Schedules.

@Composable
fun ThresholdsScreen(data: WData, go: Go) {
    val w = data.world
    val context = LocalContext.current
    var editing by remember { mutableStateOf<Threshold?>(null) }
    PracticeScaffold(
        gs(R.string.thr_schedule_title),
        { go.back() },
        actions = {
            IconButton(onClick = { editing = Threshold(roleId = w.focus?.id ?: w.roles.firstOrNull()?.id ?: 0, at = 18 * 60 + 30) }) {
                Icon(Icons.Rounded.Add, gs(R.string.add), tint = U.c.ink)
            }
        },
    ) {
        item {
            Text(gs(R.string.thr_schedule_sub), style = UType.Sub, modifier = Modifier.padding(start = 2.dp, bottom = 14.dp))
        }
        if (w.thresholds.isEmpty()) {
            item {
                EmptyState(gs(R.string.thr_schedule_empty), gs(R.string.thr_add), {
                    editing = Threshold(roleId = w.focus?.id ?: w.roles.firstOrNull()?.id ?: 0, at = 18 * 60 + 30)
                })
            }
        }
        items(w.thresholds, key = { it.id }) { t ->
            val role = w.role(t.roleId)
            val color = roleColor(w, role)
            UCard(Modifier.padding(bottom = 10.dp), onClick = { editing = t }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Facet(color, Modifier.size(36.dp), cut = role?.let { w.stats(it.id).cut.ordinal } ?: 0)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(hhmm(t.at) + "  ·  " + roleName(role), style = UType.Strong)
                        val days = daysLabel(t.days)
                        Text(listOf(t.label, days).filter { it.isNotBlank() }.joinToString(" · "), style = UType.Muted)
                    }
                    Switch(
                        checked = t.on,
                        onCheckedChange = { on -> context.container.write { upsertThreshold(t.copy(on = on)) } },
                        colors = SwitchDefaults.colors(checkedTrackColor = color, checkedThumbColor = U.c.onAccent),
                    )
                }
            }
        }
    }
    editing?.let { t -> ThresholdDialog(t, data, onDismiss = { editing = null }) }
}

@Composable
private fun daysLabel(mask: Int): String = when (mask and 0b1111111) {
    0b1111111 -> gs(R.string.thr_every_day)
    0b0011111 -> gs(R.string.thr_weekdays)
    0b1100000 -> gs(R.string.thr_weekends)
    else -> (0 until 7).filter { mask and (1 shl it) != 0 }.map { weekdayShort(it) }.joinToString(" ")
}

@Composable
private fun ThresholdDialog(initial: Threshold, data: WData, onDismiss: () -> Unit) {
    val w = data.world
    val context = LocalContext.current
    var t by remember { mutableStateOf(initial) }
    var pickRole by remember { mutableStateOf(false) }
    var pickTime by remember { mutableStateOf(false) }
    val role = w.role(t.roleId)
    val suggestions = stringArrayResource(R.array.thr_label_suggestions).toList()
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = U.c.surface2,
        title = { Text(gs(if (initial.id == 0L) R.string.thr_new else R.string.thr_edit), style = UType.Heading) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                FieldLabel(gs(R.string.thr_role))
                UCard(onClick = { pickRole = true }, padding = androidx.compose.foundation.layout.PaddingValues(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Facet(roleColor(w, role), Modifier.size(28.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(if (role != null) roleName(role) else gs(R.string.pick_role), style = UType.Strong)
                    }
                }
                FieldLabel(gs(R.string.thr_time))
                UCard(onClick = { pickTime = true }, padding = androidx.compose.foundation.layout.PaddingValues(12.dp)) {
                    Text(hhmm(t.at), style = UType.Number)
                }
                FieldLabel(gs(R.string.thr_days))
                DaysPicker(t.days, { t = t.copy(days = it) }, color = roleColor(w, role))
                FieldLabel(gs(R.string.thr_label))
                UTextField(t.label, { t = t.copy(label = it) }, placeholder = suggestions.first())
                Spacer(Modifier.height(8.dp))
                Suggestions(suggestions, { it == t.label }) { t = t.copy(label = it) }
                if (initial.id != 0L) {
                    Spacer(Modifier.height(14.dp))
                    SoftButton(gs(R.string.delete), {
                        context.container.write { deleteThreshold(initial.id) }
                        onDismiss()
                    }, color = U.c.danger, icon = Icons.Rounded.DeleteOutline)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = role != null && t.days and 0b1111111 != 0,
                onClick = {
                    val save = t
                    context.container.write { upsertThreshold(save) }
                    onDismiss()
                },
            ) { Text(gs(R.string.save), color = U.c.accent, style = UType.Button) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(gs(R.string.cancel), color = U.c.muted, style = UType.Button) } },
    )
    if (pickRole) RolePickerDialog(w, gs(R.string.pick_role), onPick = { t = t.copy(roleId = it.id) }, onDismiss = { pickRole = false })
    if (pickTime) TimeDialog(t.at, onDismiss = { pickTime = false }) { t = t.copy(at = it) }
}
