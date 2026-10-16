package app.prism.ui.practices

import androidx.activity.compose.BackHandler
import app.prism.ui.quoted
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.prism.R
import app.prism.container
import app.prism.data.Rehearsal
import app.prism.domain.lines
import app.prism.ui.Go
import app.prism.ui.WData
import app.prism.ui.components.Bar
import app.prism.ui.components.ConfirmDialog
import app.prism.ui.components.DateDialog
import app.prism.ui.components.Facet
import app.prism.ui.components.InfoRow
import app.prism.ui.components.PrimaryButton
import app.prism.ui.components.RolePickerDialog
import app.prism.ui.components.SectionTitle
import app.prism.ui.components.SoftButton
import app.prism.ui.components.UCard
import app.prism.ui.components.UTextField
import app.prism.ui.components.roleColor
import app.prism.ui.gs
import app.prism.ui.roleName
import app.prism.ui.theme.U
import app.prism.ui.theme.UType
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun RehearsalScreen(id: Long, roleId: Long, data: WData, go: Go) {
    val w = data.world
    val context = LocalContext.current
    val existing = w.rehearsals.firstOrNull { it.id == id }
    val base = existing ?: Rehearsal(roleId = roleId, title = "", createdAt = data.now)

    var title by rememberSaveable(id) { mutableStateOf(base.title) }
    var day by rememberSaveable(id) { mutableStateOf(base.day) }
    var rid by rememberSaveable(id) { mutableLongStateOf(base.roleId) }
    var goal by rememberSaveable(id) { mutableStateOf(base.goal) }
    var say by rememberSaveable(id) { mutableStateOf(base.say) }
    var notSay by rememberSaveable(id) { mutableStateOf(base.notSay) }
    var exit by rememberSaveable(id) { mutableStateOf(base.exit) }
    var outcome by rememberSaveable(id) { mutableStateOf(base.outcome) }
    var score by remember(id) { mutableFloatStateOf(if (base.outcomeScore >= 0) base.outcomeScore.toFloat() else 60f) }
    var pickRole by remember { mutableStateOf(false) }
    var pickDate by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var visual by rememberSaveable { mutableStateOf<Int?>(null) }

    val role = w.role(rid)
    val color = roleColor(w, role)

    fun current(done: Boolean = base.done, withOutcome: Boolean = false) = base.copy(
        title = title.trim(), day = day, roleId = rid, goal = goal.trim(), say = say.trim(), notSay = notSay.trim(), exit = exit.trim(),
        outcome = outcome.trim(), outcomeScore = if (withOutcome) score.toInt() else base.outcomeScore, done = done,
    )

    val v = visual
    if (v != null) {
        BackHandler { visual = null }
        Visualise(v, current(), color, role?.motto.orEmpty(), roleName(role), onStep = { visual = it }, onClose = { visual = null })
        return
    }

    PracticeScaffold(
        gs(R.string.pr_rehearsal),
        { go.back() },
        actions = {
            if (existing != null) IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Rounded.DeleteOutline, gs(R.string.delete), tint = U.c.muted) }
        },
    ) {
        item {
            Text(gs(R.string.reh_intro), style = UType.Sub, modifier = Modifier.padding(start = 2.dp, bottom = 6.dp))
            FieldLabel(gs(R.string.reh_title))
            UTextField(title, { title = it }, placeholder = gs(R.string.reh_title_hint))
            FieldLabel(gs(R.string.reh_role))
            UCard(onClick = { pickRole = true }, padding = PaddingValues(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Facet(color, Modifier.size(30.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(if (role != null) roleName(role) else gs(R.string.pick_role), style = UType.Strong)
                        if (role != null && role.motto.isNotBlank()) Text(quoted(role.motto), style = UType.Muted)
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            InfoRow(
                Icons.Rounded.Event,
                gs(R.string.reh_when),
                day?.let { LocalDate.ofEpochDay(it).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)) } ?: gs(R.string.reh_when_none),
                tint = color,
                onClick = { pickDate = true },
            )
            FieldLabel(gs(R.string.reh_goal))
            UTextField(goal, { goal = it }, placeholder = gs(R.string.reh_goal_hint), minLines = 2)
            FieldLabel(gs(R.string.reh_say))
            UTextField(say, { say = it }, placeholder = gs(R.string.reh_say_hint), minLines = 3)
            FieldLabel(gs(R.string.reh_not_say))
            UTextField(notSay, { notSay = it }, placeholder = gs(R.string.reh_not_say_hint), minLines = 2)
            FieldLabel(gs(R.string.reh_exit))
            UTextField(exit, { exit = it }, placeholder = gs(R.string.reh_exit_hint), minLines = 2)
            Spacer(Modifier.height(20.dp))
            SoftButton(gs(R.string.reh_visualise), { visual = 0 }, Modifier.fillMaxWidth(), color = color, icon = Icons.Rounded.PlayArrow)
            Spacer(Modifier.height(10.dp))
            PrimaryButton(
                gs(R.string.save),
                {
                    val r = current()
                    context.container.write { upsertRehearsal(r) }
                    go.back()
                },
                Modifier.fillMaxWidth(),
                enabled = title.isNotBlank() && rid != 0L,
            )
        }
        if (existing != null) {
            item {
                SectionTitle(gs(R.string.reh_after))
                UCard {
                    Text(gs(R.string.reh_outcome), style = UType.Muted)
                    Spacer(Modifier.height(8.dp))
                    UTextField(outcome, { outcome = it }, placeholder = gs(R.string.reh_outcome_hint), minLines = 3)
                    Spacer(Modifier.height(14.dp))
                    Text(gs(R.string.reh_score, score.toInt()), style = UType.Muted)
                    Slider(
                        score, { score = it }, valueRange = 0f..100f, steps = 9,
                        colors = SliderDefaults.colors(thumbColor = color, activeTrackColor = color, inactiveTrackColor = U.c.line),
                    )
                    Spacer(Modifier.height(8.dp))
                    PrimaryButton(
                        gs(if (existing.done) R.string.save else R.string.reh_mark_done),
                        {
                            val r = current(done = true, withOutcome = true)
                            context.container.write { upsertRehearsal(r) }
                            go.back()
                        },
                        Modifier.fillMaxWidth(),
                        color = color,
                    )
                }
            }
        }
    }

    if (pickRole) RolePickerDialog(w, gs(R.string.pick_role), onPick = { rid = it.id }, onDismiss = { pickRole = false })
    if (pickDate) DateDialog(day, onDismiss = { pickDate = false }, onClear = { day = null }) { day = it }
    if (confirmDelete && existing != null) ConfirmDialog(
        gs(R.string.reh_delete_title), gs(R.string.reh_delete_text), gs(R.string.delete),
        onConfirm = { context.container.write { deleteRehearsal(existing.id) }; go.back() },
        onDismiss = { confirmDelete = false }, danger = true,
    )
}

/** Six guided steps through the conversation before it happens. */
@Composable
private fun Visualise(step: Int, r: Rehearsal, color: Color, motto: String, roleName: String, onStep: (Int) -> Unit, onClose: () -> Unit) {
    val titles = listOf(R.string.vis_1_t, R.string.vis_2_t, R.string.vis_3_t, R.string.vis_4_t, R.string.vis_5_t, R.string.vis_6_t)
    val texts = listOf(R.string.vis_1, R.string.vis_2, R.string.vis_3, R.string.vis_4, R.string.vis_5, R.string.vis_6)
    val c = U.c
    Box(Modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(Brush.verticalGradient(listOf(color.copy(alpha = 0.16f), Color.Transparent), endY = size.height * 0.6f))
        }
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, gs(R.string.close), tint = c.muted) }
                Text(gs(R.string.reh_visualise), style = UType.Heading, modifier = Modifier.weight(1f))
                Text("${step + 1}/6", style = UType.Small, modifier = Modifier.padding(end = 16.dp))
            }
            Bar((step + 1) / 6f, Modifier.padding(horizontal = 24.dp), color = color, height = 3.dp)
            AnimatedContent(step, transitionSpec = { fadeIn() togetherWith fadeOut() }, modifier = Modifier.weight(1f), label = "vis") { s ->
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Facet(color, Modifier.size(48.dp), cut = 1)
                    Spacer(Modifier.height(18.dp))
                    Text(gs(titles[s]), style = UType.Title.copy(textAlign = TextAlign.Center))
                    Spacer(Modifier.height(14.dp))
                    Text(if (s == 2) gs(texts[s], roleName.ifBlank { "…" }) else gs(texts[s]), style = UType.Sub.copy(textAlign = TextAlign.Center))
                    val extra = when (s) {
                        0 -> r.title
                        1 -> r.goal
                        2 -> if (motto.isNotBlank()) quoted(motto) else ""
                        3 -> lines(r.say).firstOrNull().orEmpty()
                        4 -> r.exit
                        else -> lines(r.say).drop(1).joinToString("\n")
                    }
                    if (extra.isNotBlank()) {
                        Spacer(Modifier.height(22.dp))
                        Text(
                            extra,
                            style = (if (s == 2) UType.Motto else UType.Body).copy(color = if (s == 2) color else c.ink, textAlign = TextAlign.Center),
                        )
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (step > 0) SoftButton(gs(R.string.back), { onStep(step - 1) }, Modifier.weight(1f), color = c.muted)
                if (step < 5) PrimaryButton(gs(R.string.next), { onStep(step + 1) }, Modifier.weight(1f), color = color)
                else PrimaryButton(gs(R.string.done), onClose, Modifier.weight(1f), color = color)
            }
        }
    }
}
