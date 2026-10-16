package app.prism.ui.practices

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.prism.R
import app.prism.domain.Practice
import app.prism.ui.Go
import app.prism.ui.WData
import app.prism.ui.components.Bar
import app.prism.ui.components.IconDisc
import app.prism.ui.components.RolePickerDialog
import app.prism.ui.components.SectionTitle
import app.prism.ui.components.UCard
import app.prism.ui.components.practiceBlurb
import app.prism.ui.components.practiceIcon
import app.prism.ui.components.practiceName
import app.prism.ui.components.roleColor
import app.prism.ui.gp
import app.prism.ui.gs
import app.prism.ui.roleName
import app.prism.ui.theme.U
import app.prism.ui.theme.UType
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val daily = listOf(Practice.MORNING, Practice.THRESHOLD, Practice.REVIEW)
private val hard = listOf(Practice.SOS, Practice.REHEARSAL, Practice.COUNCIL)
private val deeper = listOf(Practice.LETTER, Practice.OTHERS, Practice.SPEECH, Practice.SHADOW_TALK, Practice.MODEL, Practice.ANCHOR, Practice.ACT)

@Composable
fun PracticesTab(data: WData, go: Go) {
    val w = data.world
    var pickFor by remember { mutableStateOf<Practice?>(null) }

    fun open(p: Practice) {
        when (p) {
            Practice.MORNING -> go.morning()
            Practice.REVIEW -> go.review()
            Practice.SOS -> go.sos()
            Practice.REHEARSAL -> go.rehearsal()
            Practice.COUNCIL -> go.council()
            Practice.OTHERS, Practice.SPEECH -> go.note(p)
            Practice.SHADOW_TALK -> if (w.shadows.isEmpty()) go.note(p) else pickFor = p
            else -> pickFor = p
        }
    }

    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 28.dp),
    ) {
        item {
            Text(gs(R.string.prx_title), style = UType.Display)
            Spacer(Modifier.height(4.dp))
            Text(gs(R.string.prx_sub), style = UType.Sub)
        }

        if (w.runningActs.isNotEmpty()) {
            item { SectionTitle(gs(R.string.prx_running_acts)) }
            items(w.runningActs, key = { "act" + it.id }) { act ->
                val role = w.role(act.roleId)
                val color = roleColor(w, role)
                UCard(Modifier.padding(bottom = 10.dp), onClick = { go.act(act.id) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconDisc(practiceIcon(Practice.ACT), 40.dp, color)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(roleName(role), style = UType.Strong)
                            Text(gs(R.string.prx_act_day, w.actIndex(act) + 1, act.length), style = UType.Muted)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Bar(w.actDone(act).toFloat() / act.length, color = color)
                }
            }
        }

        if (w.upcoming.isNotEmpty()) {
            item { SectionTitle(gs(R.string.prx_open_rehearsals)) }
            items(w.upcoming, key = { "reh" + it.id }) { r ->
                val role = w.role(r.roleId)
                UCard(Modifier.padding(bottom = 10.dp), onClick = { go.rehearsal(r.id) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconDisc(practiceIcon(Practice.REHEARSAL), 40.dp, roleColor(w, role))
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(r.title.ifBlank { gs(R.string.reh_untitled) }, style = UType.Strong)
                            val sub = listOfNotNull(
                                r.day?.let { LocalDate.ofEpochDay(it).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)) },
                                role?.let { roleName(it) },
                            ).joinToString(" · ")
                            if (sub.isNotEmpty()) Text(sub, style = UType.Muted)
                        }
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = U.c.faint)
                    }
                }
            }
        }

        item { SectionTitle(gs(R.string.prx_daily)) }
        items(daily) { p -> PracticeCard(p) { open(p) } }
        item {
            UCard(Modifier.padding(bottom = 10.dp), onClick = { go.thresholds() }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconDisc(Icons.Rounded.Schedule, 40.dp, U.c.muted)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(gs(R.string.thr_schedule_title), style = UType.Strong)
                        val on = w.thresholds.count { it.on }
                        Text(if (on == 0) gs(R.string.thr_schedule_none) else gp(R.plurals.thr_scheduled, on, on), style = UType.Muted)
                    }
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = U.c.faint)
                }
            }
        }
        item { SectionTitle(gs(R.string.prx_hard)) }
        items(hard) { p -> PracticeCard(p) { open(p) } }
        item { SectionTitle(gs(R.string.prx_deeper)) }
        items(deeper) { p -> PracticeCard(p) { open(p) } }
    }

    pickFor?.let { p ->
        RolePickerDialog(
            w,
            gs(if (p == Practice.SHADOW_TALK) R.string.pick_shadow else R.string.pick_role),
            shadows = p == Practice.SHADOW_TALK,
            onPick = { r ->
                when (p) {
                    Practice.THRESHOLD -> go.threshold(r.id)
                    Practice.ANCHOR -> go.anchor(r.id)
                    Practice.ACT -> w.actOf(r.id)?.let { go.act(it.id) } ?: go.act(0, r.id)
                    else -> go.note(p, 0, r.id)
                }
            },
            onDismiss = { pickFor = null },
        )
    }
}

@Composable
private fun PracticeCard(p: Practice, onClick: () -> Unit) {
    UCard(Modifier.padding(bottom = 10.dp), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconDisc(practiceIcon(p), 40.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(practiceName(p), style = UType.Strong)
                Spacer(Modifier.height(2.dp))
                Text(app.prism.ui.g(practiceBlurb(p)), style = UType.Muted)
            }
        }
    }
}
