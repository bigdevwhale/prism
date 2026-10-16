package app.prism.ui.journal

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.TheaterComedy
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.prism.R
import app.prism.container
import app.prism.data.Note
import app.prism.data.Rehearsal
import app.prism.data.Scene
import app.prism.domain.Cut
import app.prism.domain.Practice
import app.prism.ui.Go
import app.prism.ui.WData
import app.prism.ui.components.Bar
import app.prism.ui.components.ConfirmDialog
import app.prism.ui.components.Facet
import app.prism.ui.components.Pill
import app.prism.ui.components.PrismView
import app.prism.ui.components.Radar
import app.prism.ui.components.SectionTitle
import app.prism.ui.components.Segmented
import app.prism.ui.components.UCard
import app.prism.ui.components.cutName
import app.prism.ui.components.practiceIcon
import app.prism.ui.components.practiceName
import app.prism.ui.components.roleColor
import app.prism.ui.gp
import app.prism.ui.gs
import app.prism.ui.locale
import app.prism.ui.roleName
import app.prism.ui.sphereName
import app.prism.ui.theme.U
import app.prism.ui.theme.UType
import app.prism.ui.today.Dot
import app.prism.ui.today.EnergyDot
import app.prism.ui.today.beamsFor
import app.prism.ui.today.prismReading
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Something that happened on a day, for the history list. */
private sealed class Entry(val day: Long, val at: Long) {
    class OfScene(val scene: Scene) : Entry(scene.day, scene.at)
    class OfNote(val note: Note) : Entry(note.day, note.createdAt)
    class OfTalk(val talk: Rehearsal, day: Long) : Entry(day, talk.createdAt)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun JournalTab(data: WData, go: Go) {
    val w = data.world
    val context = LocalContext.current
    var range by rememberSaveable { mutableIntStateOf(7) }
    var deleting by remember { mutableStateOf<Scene?>(null) }
    val from = data.day - (range - 1)

    val history = remember(w, from) {
        val scenes = w.scenes.filter { it.day >= from }.map { Entry.OfScene(it) }
        val notes = w.notes.filter { it.day >= from }.map { Entry.OfNote(it) }
        val talks = w.rehearsals.filter { it.done }.map { Entry.OfTalk(it, it.day ?: java.time.Instant.ofEpochMilli(it.createdAt).atZone(java.time.ZoneId.systemDefault()).toLocalDate().toEpochDay()) }
            .filter { it.day >= from }
        (scenes + notes + talks).sortedWith(compareByDescending<Entry> { it.day }.thenByDescending { it.at }).groupBy { it.day }
    }

    LazyColumn(Modifier.fillMaxSize().statusBarsPadding(), contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 28.dp)) {
        item {
            Text(stringResource(R.string.tab_journal), style = UType.Title, modifier = Modifier.padding(top = 18.dp, bottom = 14.dp, start = 4.dp))
            Segmented(listOf(7, 30, 90), range, { gp(R.plurals.days, it, it) }, { range = it })
        }

        if (w.roles.isEmpty() && history.isEmpty()) {
            item {
                Spacer(Modifier.height(30.dp))
                Text(gs(R.string.journal_empty), style = UType.Sub, modifier = Modifier.padding(horizontal = 4.dp))
            }
            return@LazyColumn
        }

        // The prism over the range.
        item {
            Spacer(Modifier.height(14.dp))
            UCard(padding = PaddingValues(start = 6.dp, end = 10.dp, top = 12.dp, bottom = 14.dp)) {
                PrismView(beamsFor(w, from), Modifier.fillMaxWidth().height(190.dp), animate = false)
                Text(prismReading(w, from), style = UType.Sub, modifier = Modifier.padding(start = 10.dp))
            }
        }

        // Balance of spheres.
        val balance = w.balance(from).filter { (s, _) -> w.rolesIn(s.id).isNotEmpty() }
        if (balance.isNotEmpty()) {
            item {
                SectionTitle(stringResource(R.string.journal_balance))
                UCard {
                    val max = balance.maxOf { it.second }.coerceAtLeast(1)
                    if (balance.size >= 3) {
                        Radar(
                            balance.map { (s, n) -> U.c.hue(s.hue) to n.toFloat() / max },
                            Modifier.fillMaxWidth().height(220.dp),
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                    balance.sortedByDescending { it.second }.forEach { (s, n) ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Dot(U.c.hue(s.hue))
                            Spacer(Modifier.width(10.dp))
                            Text(sphereName(s), style = UType.Body, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(n.toString(), style = UType.Strong.copy(color = U.c.muted))
                        }
                    }
                    val total = balance.sumOf { it.second }
                    Spacer(Modifier.height(8.dp))
                    if (total == 0) Text(gs(R.string.journal_balance_none), style = UType.Muted)
                    else {
                        val least = balance.minBy { it.second }.first
                        Text(gs(R.string.journal_balance_least, sphereName(least)), style = UType.Muted)
                    }
                }
            }
        }

        // What charges and what drains.
        val energy = w.energyMap()
        if (energy.isNotEmpty()) {
            item {
                SectionTitle(stringResource(R.string.journal_energy))
                UCard {
                    energy.forEach { (r, e) ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(roleName(r), style = UType.Body, modifier = Modifier.weight(0.42f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Spacer(Modifier.width(8.dp))
                            CentreBar(e / 2f, Modifier.weight(0.58f).height(12.dp), U.c.good, U.c.danger, U.c.line)
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Row {
                        Text(stringResource(R.string.journal_drains), style = UType.Small, modifier = Modifier.weight(1f))
                        Text(stringResource(R.string.journal_charges), style = UType.Small)
                    }
                }
            }
        }

        // Shadows.
        if (w.shadows.isNotEmpty()) {
            item {
                SectionTitle(stringResource(R.string.journal_shadows))
                UCard {
                    val counts = w.shadowCounts(from).filter { it.second > 0 }
                    if (counts.isEmpty()) Text(gs(R.string.journal_shadows_none), style = UType.Muted)
                    else {
                        val max = counts.maxOf { it.second }
                        counts.forEach { (s, n) ->
                            Column(Modifier.padding(vertical = 5.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(roleName(s), style = UType.Body, modifier = Modifier.weight(1f))
                                    Text(n.toString(), style = UType.Strong.copy(color = U.c.shadow))
                                }
                                Spacer(Modifier.height(4.dp))
                                Bar(n.toFloat() / max, color = U.c.shadow)
                            }
                        }
                    }
                }
            }
        }

        // Facets.
        if (w.roles.isNotEmpty()) {
            item {
                SectionTitle(stringResource(R.string.journal_facets))
                UCard {
                    w.roles.sortedByDescending { w.stats(it.id).points }.forEach { r ->
                        val st = w.stats(r.id)
                        val color = roleColor(w, r)
                        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Facet(color, Modifier.size(30.dp), cut = st.cut.ordinal)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(roleName(r), style = UType.Strong, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(cutName(st.cut), style = UType.Small.copy(color = color))
                                }
                                Spacer(Modifier.height(5.dp))
                                Bar(Cut.progress(st.points), color = color, height = 5.dp)
                            }
                        }
                    }
                }
            }
        }

        // History.
        item { SectionTitle(stringResource(R.string.journal_history)) }
        if (history.isEmpty()) {
            item { Text(gs(R.string.journal_history_none), style = UType.Muted, modifier = Modifier.padding(horizontal = 4.dp)) }
        }
        history.forEach { (day, entries) ->
            item(key = "d$day") {
                Text(dayTitle(day, data.day), style = UType.Heading.copy(color = U.c.muted), modifier = Modifier.padding(start = 4.dp, top = 10.dp, bottom = 6.dp))
            }
            items(entries, key = { e ->
                when (e) {
                    is Entry.OfScene -> "s" + e.scene.id
                    is Entry.OfNote -> "n" + e.note.id
                    is Entry.OfTalk -> "t" + e.talk.id
                }
            }) { e ->
                when (e) {
                    is Entry.OfScene -> SceneLine(e.scene, data, Modifier.combinedClickable(onClick = { w.role(e.scene.roleId)?.let { go.role(it.id) } }, onLongClick = { deleting = e.scene }))
                    is Entry.OfNote -> NoteLine(e.note, data, go)
                    is Entry.OfTalk -> TalkLine(e.talk, data, go)
                }
            }
        }
    }

    deleting?.let { s ->
        ConfirmDialog(
            title = stringResource(R.string.journal_delete_title),
            text = stringResource(R.string.journal_delete_text),
            confirm = stringResource(R.string.delete),
            onConfirm = { context.container.write { deleteScene(s.id) } },
            onDismiss = { deleting = null },
            danger = true,
        )
    }
}

@Composable
private fun dayTitle(day: Long, today: Long): String = when (day) {
    today -> stringResource(R.string.when_today)
    today - 1 -> stringResource(R.string.when_yesterday)
    else -> LocalDate.ofEpochDay(day).format(DateTimeFormatter.ofPattern("d MMMM, EEEE", locale()))
}

@Composable
private fun SceneLine(s: Scene, data: WData, modifier: Modifier) {
    val w = data.world
    val role = w.role(s.roleId)
    val color = roleColor(w, role)
    Row(modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp), verticalAlignment = Alignment.Top) {
        Dot(color, 10.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f).padding(top = 0.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(roleName(role), style = UType.Strong, modifier = Modifier.weight(1f, fill = false), maxLines = 1, overflow = TextOverflow.Ellipsis)
                when (s.kind) {
                    Scene.THRESHOLD -> Pill(stringResource(R.string.pr_threshold), U.c.muted)
                    Scene.SOS -> Pill(stringResource(R.string.pr_sos), U.c.muted)
                    Scene.SHADOW -> Pill(stringResource(R.string.journal_noticed), U.c.shadow)
                    Scene.QUICK -> Pill(stringResource(R.string.journal_quick), U.c.muted)
                    else -> Unit
                }
            }
            val shadow = w.role(s.shadowId)
            if (shadow != null) {
                Spacer(Modifier.height(4.dp))
                Pill(roleName(shadow), U.c.shadow)
            }
            if (s.note.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(s.note, style = UType.Muted)
            }
        }
        if (s.score >= 0) {
            Spacer(Modifier.width(10.dp))
            Text("${s.score}%", style = UType.Strong.copy(color = color))
            Spacer(Modifier.width(8.dp))
            EnergyDot(s.energy, size = 16.dp, color = U.c.muted)
        }
    }
}

@Composable
private fun NoteLine(n: Note, data: WData, go: Go) {
    val kind = Practice.of(n.kind) ?: return
    val role = data.world.role(n.roleId)
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { if (kind == Practice.COUNCIL) go.council(n.id) else if (kind.written) go.note(kind, n.id, n.roleId) }
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(practiceIcon(kind), null, Modifier.size(18.dp), tint = roleColor(data.world, role).takeIf { role != null } ?: U.c.accent)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(practiceName(kind), style = UType.Strong)
            val sub = n.title.ifBlank { roleName(role) }
            if (sub.isNotBlank()) Text(sub, style = UType.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = U.c.faint)
    }
}

@Composable
private fun TalkLine(t: Rehearsal, data: WData, go: Go) {
    val role = data.world.role(t.roleId)
    Row(
        Modifier.fillMaxWidth().clickable { go.rehearsal(t.id) }.padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.TheaterComedy, null, Modifier.size(18.dp), tint = roleColor(data.world, role))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(t.title, style = UType.Strong, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(stringResource(R.string.pr_rehearsal) + if (t.outcomeScore >= 0) " · ${t.outcomeScore}%" else "", style = UType.Muted)
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = U.c.faint)
    }
}

/** A bar growing left (negative) or right (positive) from the middle; [v] is -1..1. */
@Composable
private fun CentreBar(v: Float, modifier: Modifier, plus: Color, minus: Color, track: Color) {
    Canvas(modifier) {
        val mid = size.width / 2
        val h = size.height
        drawRoundRect(track, Offset(0f, h * 0.35f), Size(size.width, h * 0.3f), androidx.compose.ui.geometry.CornerRadius(h, h))
        val len = mid * v.coerceIn(-1f, 1f)
        val color = if (v >= 0) plus else minus
        val left = if (len >= 0) mid else mid + len
        drawRoundRect(color, Offset(left, 0f), Size(kotlin.math.abs(len).coerceAtLeast(2f), h), androidx.compose.ui.geometry.CornerRadius(h / 2, h / 2))
        drawLine(track, Offset(mid, -2f), Offset(mid, h + 2f), 1.5f * density)
    }
}
