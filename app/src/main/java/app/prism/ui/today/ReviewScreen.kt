package app.prism.ui.today

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.prism.R
import app.prism.container
import app.prism.data.DayPlan
import app.prism.data.Scene
import app.prism.ui.Go
import app.prism.ui.WData
import app.prism.ui.components.Beam
import app.prism.ui.components.PrimaryButton
import app.prism.ui.components.PrismView
import app.prism.ui.components.RoleRow
import app.prism.ui.components.SectionTitle
import app.prism.ui.components.TopBar
import app.prism.ui.components.roleColor
import app.prism.ui.gs
import app.prism.ui.roleName
import app.prism.ui.sphereName
import app.prism.ui.theme.U
import app.prism.ui.theme.UType

/** The evening review: pick today's roles, rate each scene, see today's light. */
@Composable
fun ReviewScreen(data: WData, go: Go) {
    val w = data.world
    val context = LocalContext.current
    var step by rememberSaveable { mutableIntStateOf(0) }
    // Prefill from today's review if there is one; otherwise the focus role and today's thresholds.
    val existing = remember { w.scenesOn(data.day).filter { it.kind == Scene.REVIEW } }
    val selected = remember {
        mutableStateListOf<Long>().apply {
            if (existing.isNotEmpty()) addAll(existing.map { it.roleId }.distinct())
            else {
                w.focus?.let { add(it.id) }
                w.scenesOn(data.day).filter { it.kind == Scene.THRESHOLD }.map { it.roleId }.distinct()
                    .filter { id -> id !in this && w.role(id)?.shadow == false }.forEach { add(it) }
            }
        }
    }
    val drafts = remember {
        mutableStateMapOf<Long, Draft>().apply {
            existing.forEach { put(it.roleId, Draft(if (it.score >= 0) it.score else 70, it.energy, it.shadowId, it.note)) }
        }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding().imePadding()) {
        TopBar(stringResource(R.string.pr_review), onBack = { if (step == 1) step = 0 else go.back() })
        when (step) {
            0 -> {
                LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 20.dp)) {
                    item { Text(gs(R.string.review_pick), style = UType.Sub, modifier = Modifier.padding(horizontal = 4.dp)) }
                    w.spheres.forEach { s ->
                        val roles = w.rolesIn(s.id)
                        if (roles.isNotEmpty()) {
                            item(key = "s" + s.id) { SectionTitle(sphereName(s)) }
                            items(roles, key = { "r" + it.id }) { r ->
                                val on = r.id in selected
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
                                    onClick = { if (on) selected.remove(r.id) else selected.add(r.id) },
                                    trailing = {
                                        Icon(
                                            if (on) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                                            null,
                                            Modifier.size(22.dp),
                                            tint = if (on) color else U.c.faint,
                                        )
                                    },
                                )
                            }
                        }
                    }
                }
                PrimaryButton(
                    stringResource(R.string.next),
                    onClick = { step = 1 },
                    enabled = selected.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 18.dp, vertical = 12.dp),
                )
            }
            1 -> {
                val roles = selected.mapNotNull { w.role(it) }
                LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 20.dp)) {
                    item { Text(gs(R.string.review_rate), style = UType.Sub, modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 12.dp)) }
                    items(roles, key = { it.id }) { r ->
                        SceneEditor(r, w, drafts[r.id] ?: Draft(), { drafts[r.id] = it })
                        Spacer(Modifier.height(12.dp))
                    }
                }
                PrimaryButton(
                    stringResource(R.string.save),
                    onClick = {
                        val day = data.day
                        val now = System.currentTimeMillis()
                        val rows = roles.map { r ->
                            val d = drafts[r.id] ?: Draft()
                            Scene(day = day, at = now, roleId = r.id, kind = Scene.REVIEW, score = d.score, energy = d.energy, shadowId = d.shadowId, note = d.note.trim())
                        }
                        context.container.write {
                            clearReview(day)
                            rows.forEach { insertScene(it) }
                            upsertDay((day(day) ?: DayPlan(day = day)).copy(reviewed = true))
                        }
                        step = 2
                    },
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 18.dp, vertical = 12.dp),
                )
            }
            else -> {
                val roles = selected.mapNotNull { w.role(it) }
                val beams = roles.map { r ->
                    val d = drafts[r.id] ?: Draft()
                    Beam(roleColor(w, r), 1f, d.score / 100f, roleName(r))
                }
                val avg = roles.map { (drafts[it.id] ?: Draft()).score }.average().takeIf { !it.isNaN() } ?: 0.0
                Column(
                    Modifier.weight(1f).fillMaxWidth().padding(horizontal = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(Modifier.height(24.dp))
                    Text(stringResource(R.string.review_light).uppercase(), style = UType.Eyebrow)
                    PrismView(beams, Modifier.fillMaxWidth().height(220.dp))
                    Spacer(Modifier.height(16.dp))
                    Text(
                        gs(
                            when {
                                avg >= 75 -> R.string.review_kind_high
                                avg >= 45 -> R.string.review_kind_mid
                                else -> R.string.review_kind_low
                            }
                        ),
                        style = UType.Motto.copy(textAlign = TextAlign.Center),
                    )
                }
                PrimaryButton(
                    stringResource(R.string.done),
                    onClick = { go.back() },
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 18.dp, vertical = 12.dp),
                )
            }
        }
    }
}
