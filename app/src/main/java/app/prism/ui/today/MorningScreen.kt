package app.prism.ui.today

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.TheaterComedy
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.prism.R
import app.prism.container
import app.prism.data.DayPlan
import app.prism.domain.lines
import app.prism.ui.Go
import app.prism.ui.WData
import app.prism.ui.components.Chip
import app.prism.ui.components.EmptyState
import app.prism.ui.components.PrimaryButton
import app.prism.ui.components.RoleRow
import app.prism.ui.components.SectionTitle
import app.prism.ui.components.TopBar
import app.prism.ui.components.UCard
import app.prism.ui.components.UTextField
import app.prism.ui.components.roleColor
import app.prism.ui.gs
import app.prism.ui.sphereName
import app.prism.ui.theme.U
import app.prism.ui.theme.UType

/** The morning fitting: which role leads today, and one intention for it. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MorningScreen(data: WData, go: Go) {
    val w = data.world
    val context = LocalContext.current
    val plan = w.plan()
    var selected by rememberSaveable { mutableLongStateOf(plan?.focusRoleId ?: 0L) }
    var intention by rememberSaveable { mutableStateOf(plan?.intention.orEmpty()) }
    val chosen = w.role(selected)

    Column(Modifier.fillMaxSize().statusBarsPadding().imePadding()) {
        TopBar(stringResource(R.string.pr_morning), onBack = { go.back() })
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 20.dp)) {
            item {
                Text(gs(R.string.morning_lead), style = UType.Sub, modifier = Modifier.padding(horizontal = 4.dp))
            }
            w.rehearsalToday?.let { talk ->
                item {
                    Spacer(Modifier.height(14.dp))
                    UCard(onClick = { go.rehearsal(talk.id) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.TheaterComedy, null, Modifier.size(22.dp), tint = U.c.accent)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(stringResource(R.string.morning_talk_today).uppercase(), style = UType.Eyebrow)
                                Text(talk.title, style = UType.Strong)
                            }
                        }
                    }
                }
            }
            if (w.roles.isEmpty()) {
                item { EmptyState(gs(R.string.prism_empty), stringResource(R.string.prism_empty_action), { go.library() }) }
            }
            w.spheres.forEach { s ->
                val roles = w.rolesIn(s.id)
                if (roles.isNotEmpty()) {
                    item(key = "s" + s.id) { SectionTitle(sphereName(s)) }
                    items(roles, key = { "r" + it.id }) { r ->
                        val on = r.id == selected
                        val color = roleColor(w, r)
                        RoleRow(
                            r, w,
                            modifier = Modifier
                                .padding(bottom = 6.dp)
                                .clip(U.RMd)
                                .background(if (on) color.copy(alpha = 0.14f) else U.c.surface.copy(alpha = if (U.c.dark) 0.6f else 1f))
                                .border(1.dp, if (on) color.copy(alpha = 0.6f) else U.c.line, U.RMd)
                                .padding(horizontal = 12.dp),
                            showSphere = false,
                            onClick = { selected = if (on) 0L else r.id },
                            trailing = {},
                        )
                    }
                }
            }
            if (chosen != null) {
                item {
                    SectionTitle(stringResource(R.string.today_intention))
                    UTextField(intention, { intention = it }, placeholder = stringResource(R.string.morning_intention_hint), minLines = 2)
                    val ideas = lines(chosen.does)
                    if (ideas.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        Text(stringResource(R.string.morning_ideas), style = UType.Small)
                        Spacer(Modifier.height(6.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ideas.forEach { idea -> Chip(idea, selected = intention == idea, onClick = { intention = idea }, color = roleColor(w, chosen)) }
                        }
                    }
                }
            }
        }
        PrimaryButton(
            stringResource(R.string.save),
            onClick = {
                val day = data.day
                val reviewed = plan?.reviewed ?: false
                val text = intention.trim()
                val focus = selected
                context.container.write { upsertDay(DayPlan(day = day, focusRoleId = focus, intention = if (focus == 0L) "" else text, reviewed = reviewed)) }
                go.back()
            },
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 18.dp, vertical = 12.dp),
        )
    }
}
