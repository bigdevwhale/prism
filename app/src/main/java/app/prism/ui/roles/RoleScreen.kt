package app.prism.ui.roles

import androidx.compose.foundation.background
import app.prism.ui.quoted
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Anchor
import androidx.compose.material.icons.rounded.Balance
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DoorFront
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EmojiPeople
import androidx.compose.material.icons.rounded.Flare
import androidx.compose.material.icons.rounded.Mail
import androidx.compose.material.icons.rounded.RateReview
import androidx.compose.material.icons.rounded.TheaterComedy
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.prism.R
import app.prism.container
import app.prism.data.Role
import app.prism.data.Scene
import app.prism.data.deleteRole
import app.prism.domain.Cut
import app.prism.domain.Practice
import app.prism.domain.lines
import app.prism.ui.Go
import app.prism.ui.WData
import app.prism.ui.components.Bar
import app.prism.ui.components.ConfirmDialog
import app.prism.ui.components.Facet
import app.prism.ui.components.PrimaryButton
import app.prism.ui.components.RoleRow
import app.prism.ui.components.SectionTitle
import app.prism.ui.components.SoftButton
import app.prism.ui.components.TopBar
import app.prism.ui.components.UCard
import app.prism.ui.components.cutName
import app.prism.ui.components.practiceIcon
import app.prism.ui.components.practiceName
import app.prism.ui.components.roleColor
import app.prism.ui.gs
import app.prism.ui.locale
import app.prism.ui.roleName
import app.prism.ui.sphereName
import app.prism.ui.theme.U
import app.prism.ui.theme.UType
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.roundToInt

@Composable
fun RoleScreen(id: Long, data: WData, go: Go) {
    val w = data.world
    val role = w.role(id) ?: return
    val context = LocalContext.current
    var confirmDelete by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        TopBar("", onBack = { go.back() }) {
            IconButton(onClick = { go.roleForm(id = id) }) { Icon(Icons.Rounded.Edit, stringResource(R.string.edit), tint = U.c.ink) }
            IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Rounded.Delete, stringResource(R.string.delete), tint = U.c.danger) }
        }
        if (role.shadow) ShadowBody(role, data, go) else RoleBody(role, data, go)
    }
    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(if (role.shadow) R.string.rs_role_delete_shadow_title else R.string.rs_role_delete_title),
            text = gs(R.string.rs_role_delete_text),
            confirm = stringResource(R.string.delete),
            danger = true,
            onConfirm = {
                context.container.write { deleteRole(role.id) }
                go.back()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RoleBody(role: Role, data: WData, go: Go) {
    val w = data.world
    val color = roleColor(w, role)
    val stats = w.stats(role.id)
    val counters = w.shadows.filter { it.antidoteId == role.id }
    val notes = w.notes.filter { it.roleId == role.id }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = SidePad, end = SidePad, bottom = 32.dp)) {
        item { Hero(role, color, data) }
        item {
            Spacer(Modifier.height(14.dp))
            UCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(cutName(stats.cut), style = UType.Strong.copy(color = color), modifier = Modifier.weight(1f))
                    val next = Cut.entries.getOrNull(stats.cut.ordinal + 1)
                    if (next != null) Text(stringResource(R.string.rs_role_cut_next, cutName(next), next.points - stats.points), style = UType.Small)
                }
                Spacer(Modifier.height(10.dp))
                Bar(Cut.progress(stats.points), color = color)
                Spacer(Modifier.height(10.dp))
                Text(gs(R.string.rs_role_cut_hint), style = UType.Muted)
            }
        }
        item {
            Spacer(Modifier.height(14.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Action(Icons.Rounded.DoorFront, stringResource(R.string.pr_threshold), color) { go.threshold(role.id) }
                Action(Icons.Rounded.RateReview, stringResource(R.string.rs_role_after_scene), color) { go.quick(role.id) }
                Action(Icons.Rounded.TheaterComedy, stringResource(R.string.pr_rehearsal), color) { go.rehearsal(roleId = role.id) }
                Action(Icons.Rounded.Anchor, stringResource(R.string.pr_anchor), color) { go.anchor(role.id) }
                Action(Icons.Rounded.Balance, stringResource(R.string.pr_act), color) { go.act(w.actOf(role.id)?.id ?: 0, role.id) }
                Action(Icons.Rounded.Mail, stringResource(R.string.pr_letter), color) { go.note(Practice.LETTER, roleId = role.id) }
                Action(Icons.Rounded.EmojiPeople, stringResource(R.string.pr_model), color) { go.note(Practice.MODEL, roleId = role.id) }
            }
        }
        item {
            val qualities = lines(role.qualities)
            if (qualities.isNotEmpty()) {
                SectionTitle(stringResource(R.string.rs_role_qualities))
                QualityChips(qualities, color)
            }
            TextSection(stringResource(R.string.rs_role_voice), role.voice)
            DoesSection(stringResource(R.string.rs_role_does), role.does, color)
            NeverSection(stringResource(R.string.rs_role_never), role.never)
            TextSection(stringResource(R.string.rs_role_body), role.body)
            TextSection(stringResource(R.string.rs_role_practice), role.practice)
            TextSection(stringResource(R.string.rs_role_anchor), role.anchor)
            TextSection(stringResource(R.string.rs_role_model), role.model)
        }
        item {
            SectionTitle(stringResource(R.string.rs_role_stats))
            UCard {
                Row {
                    Stat(stats.scenes.toString(), stringResource(R.string.rs_role_stat_scenes), Modifier.weight(1f))
                    Stat(stats.score?.let { "${it.roundToInt()}%" } ?: "—", stringResource(R.string.rs_role_stat_score), Modifier.weight(1f))
                    Stat(stats.energy?.let { energyText(it) } ?: "—", stringResource(R.string.rs_role_stat_energy), Modifier.weight(1f))
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    stats.lastDay?.let { stringResource(R.string.rs_role_last_played, dayText(it, data.day)) } ?: gs(R.string.rs_role_never_played),
                    style = UType.Muted,
                )
            }
        }
        if (counters.isNotEmpty()) {
            item {
                SectionTitle(stringResource(R.string.rs_role_answers_shadows))
                UCard(padding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)) {
                    counters.forEachIndexed { i, sh ->
                        if (i > 0) HorizontalDivider(color = U.c.line)
                        RoleRow(sh, w, onClick = { go.role(sh.id) })
                    }
                }
            }
        }
        if (notes.isNotEmpty()) {
            item {
                SectionTitle(stringResource(R.string.rs_role_notes))
                UCard(padding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)) {
                    notes.forEachIndexed { i, n ->
                        if (i > 0) HorizontalDivider(color = U.c.line)
                        val kind = Practice.of(n.kind)
                        NoteRow(
                            icon = kind?.let { practiceIcon(it) } ?: Icons.Rounded.Mail,
                            title = n.title.ifBlank { kind?.let { practiceName(it) }.orEmpty() },
                            sub = dayText(n.day, data.day),
                        ) {
                            when {
                                kind == Practice.COUNCIL -> go.council(n.id)
                                kind != null && kind.written -> go.note(kind, n.id, role.id)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Hero(role: Role, color: Color, data: WData) {
    val w = data.world
    Box(
        Modifier
            .fillMaxWidth()
            .clip(U.RXl)
            .background(Brush.verticalGradient(listOf(color.copy(alpha = 0.32f), color.copy(alpha = 0.06f))))
            .border(1.dp, color.copy(alpha = 0.4f), U.RXl)
            .padding(22.dp),
    ) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Facet(color, Modifier.size(84.dp), cut = w.stats(role.id).cut.ordinal, shadow = role.shadow)
            Spacer(Modifier.height(14.dp))
            val sphere = w.sphere(role.sphereId)
            if (sphere != null) Text(sphereName(sphere).uppercase(), style = UType.Eyebrow.copy(color = color))
            Spacer(Modifier.height(4.dp))
            Text(roleName(role), style = UType.Title.copy(textAlign = androidx.compose.ui.text.style.TextAlign.Center))
            if (role.tagline.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(role.tagline, style = UType.Sub.copy(textAlign = androidx.compose.ui.text.style.TextAlign.Center))
            }
            if (!role.shadow && role.motto.isNotBlank()) {
                Spacer(Modifier.height(14.dp))
                Text(quoted(role.motto), style = UType.Motto.copy(textAlign = androidx.compose.ui.text.style.TextAlign.Center))
            }
        }
    }
}

@Composable
private fun ShadowBody(role: Role, data: WData, go: Go) {
    val w = data.world
    val context = LocalContext.current
    val stats = w.stats(role.id)
    val month = w.scenes.count { it.day >= data.day - 29 && (it.shadowId == role.id || (it.kind == Scene.SHADOW && it.roleId == role.id)) }
    val antidote = w.role(role.antidoteId)
    var noticed by remember { mutableStateOf(false) }
    LaunchedEffect(noticed) {
        if (noticed) {
            delay(3000)
            noticed = false
        }
    }
    Column(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(start = SidePad, end = SidePad, bottom = 20.dp)) {
            item { Hero(role, U.c.shadow, data) }
            item {
                Spacer(Modifier.height(14.dp))
                UCard {
                    Row {
                        Stat(stats.appearances.toString(), stringResource(R.string.rs_shadow_stat_all), Modifier.weight(1f))
                        Stat(month.toString(), stringResource(R.string.rs_shadow_stat_month), Modifier.weight(1f))
                    }
                }
            }
            item {
                TextSection(stringResource(R.string.rs_shadow_triggers), role.triggers)
                TextSection(stringResource(R.string.rs_shadow_protects), role.protects)
                TextSection(stringResource(R.string.rs_shadow_needs), role.needs)
                TextSection(stringResource(R.string.rs_shadow_signs), role.signs)
                TextSection(stringResource(R.string.rs_shadow_answer), role.answer)
            }
            if (antidote != null) {
                item {
                    SectionTitle(stringResource(R.string.rs_shadow_antidote))
                    UCard(padding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)) {
                        RoleRow(antidote, w, onClick = { go.role(antidote.id) })
                    }
                }
            }
            item {
                Spacer(Modifier.height(18.dp))
                SoftButton(stringResource(R.string.pr_shadow_talk), { go.note(Practice.SHADOW_TALK, roleId = role.id) }, Modifier.fillMaxWidth(), color = U.c.shadow, icon = Icons.Rounded.Flare)
            }
        }
        if (noticed) {
            Text(
                gs(R.string.rs_shadow_noticed_done),
                style = UType.Body.copy(color = U.c.ink),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = SidePad)
                    .clip(U.RMd)
                    .background(U.c.shadow.copy(alpha = 0.22f))
                    .padding(14.dp),
            )
        }
        PrimaryButton(
            gs(R.string.rs_shadow_noticed),
            onClick = {
                val now = System.currentTimeMillis()
                val day = data.day
                context.container.write { insertScene(Scene(day = day, at = now, roleId = role.id, kind = Scene.SHADOW)) }
                noticed = true
            },
            color = U.c.shadow,
            icon = Icons.Rounded.Visibility,
            modifier = Modifier.fillMaxWidth().padding(horizontal = SidePad, vertical = 12.dp).navigationBarsPadding(),
        )
    }
}

@Composable
private fun Action(icon: ImageVector, label: String, color: Color, onClick: () -> Unit) {
    Row(
        Modifier
            .clip(U.RSm)
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.3f), U.RSm)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, Modifier.size(17.dp), tint = color)
        Spacer(Modifier.width(6.dp))
        Text(label, style = UType.Button.copy(color = color, fontSize = UType.Muted.fontSize))
    }
}

@Composable
private fun NoteRow(icon: ImageVector, title: String, sub: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(20.dp), tint = U.c.muted)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = UType.Strong, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(sub, style = UType.Small)
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = U.c.faint)
    }
}

/** "+1.2" / "−0.5" / "0". */
internal fun energyText(e: Float): String {
    val r = (e * 10).roundToInt() / 10f
    return when {
        r > 0f -> "+$r"
        r < 0f -> "−${-r}"
        else -> "0"
    }
}

/** Today, yesterday, or a date. */
@Composable
internal fun dayText(day: Long, today: Long): String = when (day) {
    today -> stringResource(R.string.rs_day_today)
    today - 1 -> stringResource(R.string.rs_day_yesterday)
    else -> LocalDate.ofEpochDay(day).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale()))
}
