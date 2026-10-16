package app.prism.ui.today

import androidx.compose.foundation.background
import app.prism.ui.quoted
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DoorFront
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Landscape
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.TheaterComedy
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.prism.R
import app.prism.container
import app.prism.data.Act
import app.prism.data.ActDay
import app.prism.data.actTasks
import app.prism.domain.World
import app.prism.ui.Go
import app.prism.ui.LocalPerson
import app.prism.ui.Tab
import app.prism.ui.WData
import app.prism.ui.components.Beam
import app.prism.ui.components.EmptyState
import app.prism.ui.components.Facet
import app.prism.ui.components.PrismView
import app.prism.ui.components.RolePickerDialog
import app.prism.ui.components.SectionTitle
import app.prism.ui.components.SoftButton
import app.prism.ui.components.UCard
import app.prism.ui.components.roleColor
import app.prism.ui.g
import app.prism.ui.gp
import app.prism.ui.gs
import app.prism.ui.library
import app.prism.ui.locale
import app.prism.ui.roleName
import app.prism.ui.theme.U
import app.prism.ui.theme.UType
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/** Beams of the prism for a stretch of days: one per role, wider for more scenes, brighter for better ones. */
@Composable
fun beamsFor(w: World, from: Long, to: Long = w.day): List<Beam> = w.rays(from, to).map { r ->
    Beam(
        color = roleColor(w, r.role),
        weight = 1f + r.scenes,
        brightness = r.score?.div(100f) ?: 0.25f,
        label = roleName(r.role),
    )
}

/** One line reading the week's light: the brightest role and the dimmest one that was played. */
@Composable
fun prismReading(w: World, from: Long): String {
    val rated = w.rays(from, includeIdle = false).filter { it.score != null }.sortedByDescending { it.score }
    return when {
        rated.isEmpty() -> gs(R.string.prism_reading_none)
        rated.size == 1 -> gs(R.string.prism_reading_one, roleName(rated.first().role))
        else -> gs(R.string.prism_reading, roleName(rated.first().role), roleName(rated.last().role))
    }
}

@Composable
fun PrismTab(data: WData, go: Go, onTab: (Tab) -> Unit) {
    val w = data.world
    var pickThreshold by remember { mutableStateOf(false) }
    val hour = LocalTime.now().hour
    val name = LocalPerson.current.name.trim()

    LazyColumn(Modifier.fillMaxSize().statusBarsPadding(), contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 28.dp)) {
        item {
            Row(Modifier.fillMaxWidth().padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(greeting(hour, name), style = UType.Title)
                    Text(
                        data.today.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", locale())).replaceFirstChar { it.titlecase(locale()) },
                        style = UType.Muted,
                    )
                }
                IconButton(onClick = { go.settings() }) { Icon(Icons.Rounded.Settings, stringResource(R.string.settings), tint = U.c.muted) }
            }
        }

        if (w.roles.isEmpty()) {
            item {
                Spacer(Modifier.height(24.dp))
                UCard {
                    EmptyState(gs(R.string.prism_empty), stringResource(R.string.prism_empty_action), { go.library() })
                    Text(
                        stringResource(R.string.prism_empty_own),
                        style = UType.Small.copy(color = U.c.accent),
                        modifier = Modifier.align(Alignment.CenterHorizontally).clip(U.RSm).clickable { onTab(Tab.ROLES) }.padding(8.dp),
                    )
                }
            }
            return@LazyColumn
        }

        // This week's light.
        item {
            Spacer(Modifier.height(16.dp))
            UCard(padding = PaddingValues(start = 6.dp, end = 10.dp, top = 14.dp, bottom = 14.dp)) {
                Text(stringResource(R.string.prism_week).uppercase(), style = UType.Eyebrow, modifier = Modifier.padding(start = 10.dp))
                PrismView(beamsFor(w, data.day - 6), Modifier.fillMaxWidth().height(200.dp))
                Text(prismReading(w, data.day - 6), style = UType.Sub, modifier = Modifier.padding(start = 10.dp))
            }
        }

        // Today's role.
        item {
            SectionTitle(stringResource(R.string.today_role))
            val focus = w.focus
            if (focus == null) {
                UCard(onClick = { go.morning() }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.WbSunny, null, Modifier.size(26.dp), tint = U.c.accent)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.today_who), style = UType.Heading)
                            Text(stringResource(R.string.today_who_sub), style = UType.Muted)
                        }
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = U.c.faint)
                    }
                }
            } else {
                val color = roleColor(w, focus)
                UCard(border = color.copy(alpha = 0.45f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Facet(color, Modifier.size(44.dp), cut = w.stats(focus.id).cut.ordinal)
                        Spacer(Modifier.width(14.dp))
                        Text(roleName(focus), style = UType.Heading.copy(fontSize = 20.sp), modifier = Modifier.weight(1f).clickable { go.role(focus.id) })
                    }
                    if (focus.motto.isNotBlank()) {
                        Spacer(Modifier.height(12.dp))
                        Text(quoted(focus.motto), style = UType.Motto)
                    }
                    val intention = w.plan()?.intention.orEmpty()
                    if (intention.isNotBlank()) {
                        Spacer(Modifier.height(10.dp))
                        Text(stringResource(R.string.today_intention).uppercase(), style = UType.Eyebrow)
                        Spacer(Modifier.height(2.dp))
                        Text(intention, style = UType.Body)
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SoftButton(stringResource(R.string.pr_threshold), { go.threshold(focus.id) }, Modifier.weight(1f), color = color, icon = Icons.Rounded.DoorFront)
                        SoftButton(stringResource(R.string.today_change), { go.morning() }, Modifier.weight(1f), color = U.c.muted)
                    }
                }
            }
        }

        // Quick actions.
        item {
            SectionTitle(stringResource(R.string.today_actions))
            val reviewed = w.reviewed()
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ActionTile(Icons.Rounded.DoorFront, stringResource(R.string.pr_threshold), stringResource(R.string.home_threshold_sub), Modifier.weight(1f)) { pickThreshold = true }
                    ActionTile(Icons.Rounded.Landscape, stringResource(R.string.pr_sos), stringResource(R.string.home_sos_sub), Modifier.weight(1f)) { go.sos() }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ActionTile(Icons.Rounded.EditNote, stringResource(R.string.home_quick), stringResource(R.string.home_quick_sub), Modifier.weight(1f)) { go.quick() }
                    ActionTile(
                        Icons.Rounded.Bedtime,
                        stringResource(R.string.pr_review),
                        if (reviewed) stringResource(R.string.home_review_done) else stringResource(R.string.home_review_sub),
                        Modifier.weight(1f),
                        highlight = !reviewed && hour >= 18,
                        done = reviewed,
                    ) { go.review() }
                }
            }
        }

        // Running acts.
        val acts = w.runningActs
        if (acts.isNotEmpty()) {
            item { SectionTitle(stringResource(R.string.pr_act)) }
            items(acts, key = { "act" + it.id }) { act ->
                ActCard(act, data, go)
                Spacer(Modifier.height(10.dp))
            }
        }

        // The next rehearsal.
        val talk = w.rehearsalToday ?: w.upcoming.firstOrNull()
        if (talk != null) {
            item {
                SectionTitle(stringResource(R.string.pr_rehearsal))
                val role = w.role(talk.roleId)
                UCard(onClick = { go.rehearsal(talk.id) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.TheaterComedy, null, Modifier.size(24.dp), tint = roleColor(w, role))
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(talk.title, style = UType.Strong, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            val whenText = when (talk.day) {
                                null -> ""
                                data.day -> stringResource(R.string.when_today)
                                data.day + 1 -> stringResource(R.string.when_tomorrow)
                                else -> LocalDate.ofEpochDay(talk.day).format(DateTimeFormatter.ofPattern("d MMMM", locale()))
                            }
                            val sub = listOf(whenText, roleName(role)).filter { it.isNotBlank() }.joinToString(" · ")
                            if (sub.isNotBlank()) Text(sub, style = UType.Muted)
                        }
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = U.c.faint)
                    }
                }
            }
        }

        // Streak and the core.
        item {
            Spacer(Modifier.height(22.dp))
            val streak = w.streak
            if (streak > 1) {
                Text(gp(R.plurals.home_streak, streak, streak), style = UType.Muted, modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp))
            }
            if (w.core.isNotEmpty()) {
                Row(
                    Modifier.fillMaxWidth().clip(U.RSm).clickable { go.core() }.padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(6.dp).clip(CircleShape).background(U.c.accent))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        w.core.joinToString("  ·  ") { it.text },
                        style = UType.Small.copy(color = U.c.muted),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }

    if (pickThreshold) {
        RolePickerDialog(w, stringResource(R.string.pick_role), onPick = { go.threshold(it.id) }, onDismiss = { pickThreshold = false })
    }
}

@Composable
private fun greeting(hour: Int, name: String): String {
    val withName = name.isNotEmpty()
    return when (hour) {
        in 5..11 -> if (withName) gs(R.string.greet_morning_n, name) else gs(R.string.greet_morning)
        in 12..17 -> if (withName) gs(R.string.greet_day_n, name) else gs(R.string.greet_day)
        in 18..22 -> if (withName) gs(R.string.greet_evening_n, name) else gs(R.string.greet_evening)
        else -> if (withName) gs(R.string.greet_night_n, name) else gs(R.string.greet_night)
    }
}

@Composable
private fun ActionTile(
    icon: ImageVector,
    title: String,
    sub: String,
    modifier: Modifier = Modifier,
    highlight: Boolean = false,
    done: Boolean = false,
    onClick: () -> Unit,
) {
    val tint = when {
        highlight -> U.c.accent
        done -> U.c.good
        else -> U.c.ink
    }
    Column(
        modifier
            .clip(U.RMd)
            .background(if (highlight) U.c.accentSoft else U.c.surface.copy(alpha = if (U.c.dark) 0.82f else 1f))
            .border(1.dp, if (highlight) U.c.accent.copy(alpha = 0.6f) else U.c.line, U.RMd)
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        Icon(icon, null, Modifier.size(24.dp), tint = tint)
        Spacer(Modifier.height(10.dp))
        Text(title, style = UType.Strong, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(sub, style = UType.Small.copy(color = if (done) U.c.good else U.c.faint), maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun ActCard(act: Act, data: WData, go: Go) {
    val w = data.world
    val context = LocalContext.current
    val role = w.role(act.roleId)
    val color = roleColor(w, role)
    val generic = stringArrayResource(R.array.act_generic_tasks).map { g(it) }
    val tasks = actTasks(act, role, library(), generic).map { g(it) }
    val idx = w.actIndex(act)
    val task = tasks.getOrNull(Math.floorMod(idx, tasks.size.coerceAtLeast(1))).orEmpty()
    val done = w.actDoneOn(act, data.day)
    UCard(onClick = { go.act(act.id) }, border = color.copy(alpha = 0.35f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Facet(color, Modifier.size(30.dp), cut = role?.let { w.stats(it.id).cut.ordinal } ?: 0)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(roleName(role), style = UType.Strong)
                Text(stringResource(R.string.home_act_day, idx + 1, act.length), style = UType.Small)
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(task, style = UType.Body, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(12.dp))
            Box(
                Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(if (done) color else Color.Transparent)
                    .border(1.5.dp, if (done) color else U.c.line, CircleShape)
                    .clickable {
                        val now = System.currentTimeMillis()
                        context.container.write {
                            if (done) deleteActDay(act.id, data.day) else insertActDay(ActDay(act.id, data.day, now))
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                if (done) Icon(Icons.Rounded.Check, stringResource(R.string.done), Modifier.size(20.dp), tint = U.c.onAccent)
            }
        }
    }
}
