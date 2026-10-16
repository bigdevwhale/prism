package app.prism.ui.practices

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.prism.R
import app.prism.container
import app.prism.data.Act
import app.prism.data.ActDay
import app.prism.data.actTasks
import app.prism.data.deleteAct
import app.prism.ui.Go
import app.prism.ui.WData
import app.prism.ui.components.Bar
import app.prism.ui.components.Chip
import app.prism.ui.components.ConfirmDialog
import app.prism.ui.components.Facet
import app.prism.ui.components.PrimaryButton
import app.prism.ui.components.RolePickerDialog
import app.prism.ui.components.SectionTitle
import app.prism.ui.components.SoftButton
import app.prism.ui.components.UCard
import app.prism.ui.components.UTextField
import app.prism.ui.components.cutName
import app.prism.ui.components.roleColor
import app.prism.ui.g
import app.prism.ui.gs
import app.prism.ui.library
import app.prism.ui.roleName
import app.prism.ui.theme.U
import app.prism.ui.theme.UType

@Composable
fun ActScreen(id: Long, roleId: Long, data: WData, go: Go) {
    val existing = data.world.acts.firstOrNull { it.id == id }
    if (existing != null) ActView(existing, data, go) else NewAct(roleId, data, go)
}

@Composable
private fun genericTasks(): List<String> = stringArrayResource(R.array.act_generic_tasks).map { g(it) }

@Composable
private fun NewAct(roleId: Long, data: WData, go: Go) {
    val w = data.world
    val context = LocalContext.current
    var rid by rememberSaveable { mutableLongStateOf(roleId) }
    var length by rememberSaveable { mutableIntStateOf(21) }
    var pickRole by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { if (w.role(rid) == null) pickRole = true }
    val role = w.role(rid)
    val color = roleColor(w, role)
    val lib = library()
    val generic = genericTasks()
    val defaults = remember(rid, lib) { actTasks(Act(roleId = rid, startDay = data.day), role, lib, generic).joinToString("\n") }
    var tasks by rememberSaveable(rid) { mutableStateOf(defaults) }
    val running = role?.let { w.actOf(it.id) }

    PracticeScaffold(gs(R.string.act_new), { go.back() }) {
        item {
            Text(gs(R.string.pr_act_blurb), style = UType.Sub, modifier = Modifier.padding(start = 2.dp, bottom = 4.dp))
            FieldLabel(gs(R.string.reh_role))
            UCard(onClick = { pickRole = true }, padding = PaddingValues(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Facet(color, Modifier.size(30.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(if (role != null) roleName(role) else gs(R.string.pick_role), style = UType.Strong)
                }
            }
            FieldLabel(gs(R.string.act_length))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(7, 14, 21).forEach { n -> Chip(app.prism.ui.gp(R.plurals.days, n, n), length == n, { length = n }, color = color) }
            }
            FieldLabel(gs(R.string.act_tasks))
            Text(gs(R.string.act_tasks_hint), style = UType.Small, modifier = Modifier.padding(start = 2.dp, bottom = 8.dp))
            UTextField(tasks, { tasks = it }, minLines = 7)
            Spacer(Modifier.height(20.dp))
            if (running != null) {
                Text(gs(R.string.act_already), style = UType.Muted, modifier = Modifier.padding(bottom = 10.dp))
                SoftButton(gs(R.string.act_open_running), { go.back(); go.act(running.id) }, Modifier.fillMaxWidth(), color = color)
            } else {
                PrimaryButton(
                    gs(R.string.act_start),
                    {
                        val own = if (tasks.trim() == defaults.trim()) "" else tasks.trim()
                        val act = Act(roleId = rid, startDay = data.day, length = length, tasks = own)
                        context.container.write { upsertAct(act) }
                        go.back()
                    },
                    Modifier.fillMaxWidth(),
                    enabled = role != null && tasks.isNotBlank(),
                    color = color,
                )
            }
        }
    }
    if (pickRole) RolePickerDialog(w, gs(R.string.pick_role), onPick = { rid = it.id }, onDismiss = { pickRole = false })
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActView(act: Act, data: WData, go: Go) {
    val w = data.world
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val role = w.role(act.roleId)
    val color = roleColor(w, role)
    val tasks = actTasks(act, role, library(), genericTasks())
    val running = w.actRunning(act)
    val idx = w.actIndex(act)
    val doneCount = w.actDone(act)
    var confirmDelete by remember { mutableStateOf(false) }
    var confirmEnd by remember { mutableStateOf(false) }

    fun toggle(day: Long) {
        val on = w.actDoneOn(act, day)
        if (!on) haptic.tick(data.settings.haptics)
        val now = System.currentTimeMillis()
        context.container.write { if (on) deleteActDay(act.id, day) else insertActDay(ActDay(act.id, day, now)) }
    }

    PracticeScaffold(
        gs(R.string.pr_act) + " · " + roleName(role),
        { go.back() },
        actions = { IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Rounded.DeleteOutline, gs(R.string.delete), tint = U.c.muted) } },
    ) {
        if (w.actCompleted(act)) item {
            UCard(Modifier.padding(bottom = 14.dp), border = color.copy(alpha = 0.6f)) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Facet(color, Modifier.size(72.dp), cut = role?.let { w.stats(it.id).cut.ordinal } ?: 3)
                    Spacer(Modifier.height(12.dp))
                    Text(gs(R.string.act_complete_t), style = UType.Title.copy(textAlign = TextAlign.Center))
                    Spacer(Modifier.height(6.dp))
                    Text(gs(R.string.act_complete, roleName(role), doneCount, act.length), style = UType.Sub.copy(textAlign = TextAlign.Center))
                    role?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(cutName(w.stats(it.id).cut), style = UType.Eyebrow.copy(color = color))
                    }
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        when {
                            running -> gs(R.string.prx_act_day, idx + 1, act.length)
                            act.endedAt != null -> gs(R.string.act_ended)
                            else -> gs(R.string.act_over)
                        },
                        style = UType.Heading,
                    )
                    Text(gs(R.string.act_done_of, doneCount, act.length), style = UType.Muted)
                }
                if (role != null && role.motto.isNotBlank()) Text("«${role.motto}»", style = UType.Motto.copy(color = color), modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            Bar(doneCount.toFloat() / act.length, color = color)
        }
        if (running) item {
            val day = data.day
            val done = w.actDoneOn(act, day)
            SectionTitle(gs(R.string.act_today))
            UCard(border = if (done) color.copy(alpha = 0.6f) else U.c.line, onClick = { toggle(day) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(30.dp).clip(U.RSm).background(if (done) color else U.c.surface2).border(1.dp, color.copy(alpha = 0.6f), U.RSm),
                        contentAlignment = Alignment.Center,
                    ) { if (done) Icon(Icons.Rounded.Check, null, Modifier.size(18.dp), tint = U.c.onAccent) }
                    Spacer(Modifier.width(14.dp))
                    Text(tasks[idx % tasks.size], style = UType.Body, modifier = Modifier.weight(1f))
                }
            }
        }
        item {
            SectionTitle(gs(R.string.act_days))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                (0 until act.length).forEach { i ->
                    val day = act.startDay + i
                    val done = w.actDoneOn(act, day)
                    val today = day == data.day
                    val past = day < data.day
                    val bg = when {
                        done -> color
                        past -> U.c.surface2
                        else -> U.c.surface.copy(alpha = 0.6f)
                    }
                    Box(
                        Modifier
                            .size(38.dp)
                            .clip(U.RSm)
                            .background(bg)
                            .border(if (today) 2.dp else 1.dp, if (today) color else U.c.line, U.RSm)
                            .clickable(enabled = (past || today) && (running || act.endedAt == null)) { toggle(day) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            (i + 1).toString(),
                            style = UType.Small.copy(color = when { done -> U.c.onAccent; past -> U.c.danger.copy(alpha = 0.7f); else -> U.c.muted }),
                        )
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(gs(R.string.act_days_hint), style = UType.Small)
        }
        item { SectionTitle(gs(R.string.act_tasks)) }
        tasks.forEachIndexed { i, t ->
            item {
                Row(Modifier.padding(vertical = 6.dp)) {
                    Text("${i + 1}.", style = UType.Muted.copy(color = color), modifier = Modifier.width(26.dp))
                    Text(t, style = UType.Body.copy(color = if (running && i == idx % tasks.size) U.c.ink else U.c.muted))
                }
            }
        }
        if (running) item {
            Spacer(Modifier.height(18.dp))
            SoftButton(gs(R.string.act_end), { confirmEnd = true }, Modifier.fillMaxWidth(), color = U.c.muted)
        }
    }

    if (confirmEnd) ConfirmDialog(
        gs(R.string.act_end_title), gs(R.string.act_end_text), gs(R.string.act_end),
        onConfirm = { val a = act.copy(endedAt = System.currentTimeMillis()); context.container.write { upsertAct(a) } },
        onDismiss = { confirmEnd = false },
    )
    if (confirmDelete) ConfirmDialog(
        gs(R.string.act_delete_title), gs(R.string.act_delete_text), gs(R.string.delete),
        onConfirm = { context.container.write { deleteAct(act.id) }; go.back() },
        onDismiss = { confirmDelete = false }, danger = true,
    )
}
