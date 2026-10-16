package app.prism.ui.practices

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.prism.R
import app.prism.container
import app.prism.domain.lines
import app.prism.ui.Go
import app.prism.ui.WData
import app.prism.ui.components.Bar
import app.prism.ui.components.EmptyState
import app.prism.ui.components.PrimaryButton
import app.prism.ui.components.ProgressRing
import app.prism.ui.components.SoftButton
import app.prism.ui.components.UCard
import app.prism.ui.components.UTextField
import app.prism.ui.components.roleColor
import app.prism.ui.g
import app.prism.ui.gs
import app.prism.ui.roleName
import app.prism.ui.theme.U
import app.prism.ui.theme.UType
import kotlinx.coroutines.delay

private const val CHARGE_MS = 10_000f

@Composable
fun AnchorScreen(roleId: Long, data: WData, go: Go) {
    val w = data.world
    val role = w.role(roleId)
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    if (role == null) {
        PracticeScaffold(gs(R.string.pr_anchor), { go.back() }) { item { EmptyState(gs(R.string.picker_no_roles)) } }
        return
    }
    val color = roleColor(w, role)
    val old = lines(role.anchor)
    var step by rememberSaveable { mutableIntStateOf(0) }
    var gesture by rememberSaveable { mutableStateOf(old.getOrElse(0) { "" }) }
    var word by rememberSaveable { mutableStateOf(old.getOrElse(1) { "" }) }
    var memory by rememberSaveable { mutableStateOf(old.getOrElse(2) { "" }) }
    var charge by remember { mutableFloatStateOf(0f) }
    var holding by remember { mutableStateOf(false) }
    var charged by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(holding) {
        if (!holding) { if (!charged) charge = 0f; return@LaunchedEffect }
        val start = System.currentTimeMillis()
        while (holding && charge < 1f) {
            charge = ((System.currentTimeMillis() - start) / CHARGE_MS).coerceAtMost(1f)
            delay(30)
        }
        if (charge >= 1f && !charged) {
            charged = true
            haptic.thump(data.settings.haptics)
        }
    }

    fun save() {
        val r = role.copy(anchor = listOf(gesture, word, memory).joinToString("\n") { it.trim().replace('\n', ' ') })
        context.container.write { upsertRole(r) }
        go.back()
    }

    PracticeScaffold(gs(R.string.pr_anchor) + " · " + roleName(role), { go.back() }) {
        item {
            Bar((step + 1) / 4f, color = color, height = 3.dp)
            Spacer(Modifier.height(18.dp))
            when (step) {
                0 -> {
                    Text(gs(R.string.anc_gesture_t), style = UType.Title)
                    Spacer(Modifier.height(8.dp))
                    Text(gs(R.string.anc_gesture), style = UType.Sub)
                    Spacer(Modifier.height(14.dp))
                    Suggestions(stringArrayResource(R.array.anchor_gestures).map { g(it) }, { it == gesture }) { gesture = it }
                    Spacer(Modifier.height(10.dp))
                    UTextField(gesture, { gesture = it }, placeholder = gs(R.string.anc_gesture_hint))
                }
                1 -> {
                    Text(gs(R.string.anc_word_t), style = UType.Title)
                    Spacer(Modifier.height(8.dp))
                    Text(gs(R.string.anc_word), style = UType.Sub)
                    Spacer(Modifier.height(14.dp))
                    if (role.motto.isNotBlank()) Suggestions(listOf(role.motto), { it == word }) { word = it }
                    Spacer(Modifier.height(10.dp))
                    UTextField(word, { word = it }, placeholder = gs(R.string.anc_word_hint))
                }
                2 -> {
                    Text(gs(R.string.anc_memory_t), style = UType.Title)
                    Spacer(Modifier.height(8.dp))
                    Text(gs(R.string.anc_memory, roleName(role)), style = UType.Sub)
                    Spacer(Modifier.height(14.dp))
                    UTextField(memory, { memory = it }, placeholder = gs(R.string.anc_memory_hint), minLines = 4)
                }
                else -> {
                    Text(gs(R.string.anc_charge_t), style = UType.Title)
                    Spacer(Modifier.height(8.dp))
                    Text(gs(R.string.anc_charge), style = UType.Sub)
                    Spacer(Modifier.height(16.dp))
                    UCard(border = color.copy(alpha = 0.4f)) {
                        if (gesture.isNotBlank()) Text(gesture, style = UType.Strong)
                        if (word.isNotBlank()) Text("«${word.trim()}»", style = UType.Motto.copy(color = color), modifier = Modifier.padding(top = 6.dp))
                        if (memory.isNotBlank()) Text(memory, style = UType.Body, modifier = Modifier.padding(top = 8.dp))
                    }
                    Spacer(Modifier.height(26.dp))
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        ProgressRing(charge, Modifier.size(150.dp), stroke = 6.dp, color = color) {
                            Box(
                                Modifier
                                    .size(110.dp)
                                    .clip(CircleShape)
                                    .background(color.copy(alpha = 0.10f + 0.25f * charge))
                                    .pointerInput(Unit) {
                                        detectTapGestures(onPress = {
                                            holding = true
                                            tryAwaitRelease()
                                            holding = false
                                        })
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    gs(if (charged) R.string.anc_charged else R.string.anc_hold),
                                    style = UType.Button.copy(color = U.c.ink, textAlign = TextAlign.Center),
                                    modifier = Modifier.padding(10.dp),
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(26.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (step > 0) SoftButton(gs(R.string.back), { step-- }, Modifier.weight(1f), color = U.c.muted)
                if (step < 3) PrimaryButton(
                    gs(R.string.next), { step++ }, Modifier.weight(1f),
                    enabled = when (step) { 0 -> gesture.isNotBlank(); 1 -> word.isNotBlank(); else -> true },
                    color = color,
                )
                else PrimaryButton(gs(R.string.save), { save() }, Modifier.weight(1f), color = color)
            }
        }
    }
}
