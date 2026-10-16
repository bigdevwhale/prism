package app.prism.ui.roles

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.prism.R
import app.prism.container
import app.prism.data.Role
import app.prism.ui.Go
import app.prism.ui.WData
import app.prism.ui.components.Chip
import app.prism.ui.components.Facet
import app.prism.ui.components.PrimaryButton
import app.prism.ui.components.RolePickerDialog
import app.prism.ui.components.RoleRow
import app.prism.ui.components.SectionTitle
import app.prism.ui.components.SoftButton
import app.prism.ui.components.TopBar
import app.prism.ui.components.UCard
import app.prism.ui.components.UTextField
import app.prism.ui.gs
import app.prism.ui.sphereIcon
import app.prism.ui.sphereName
import app.prism.ui.theme.U
import app.prism.ui.theme.UType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RoleFormScreen(id: Long, sphereId: Long, shadow: Boolean, data: WData, go: Go) {
    val w = data.world
    val existing = if (id != 0L) w.role(id) else null
    if (id != 0L && existing == null) return
    val isShadow = existing?.shadow ?: shadow
    val context = LocalContext.current
    val container = context.container

    var name by rememberSaveable { mutableStateOf(existing?.name.orEmpty()) }
    var tagline by rememberSaveable { mutableStateOf(existing?.tagline.orEmpty()) }
    var qualities by rememberSaveable { mutableStateOf(existing?.qualities.orEmpty()) }
    var voice by rememberSaveable { mutableStateOf(existing?.voice.orEmpty()) }
    var does by rememberSaveable { mutableStateOf(existing?.does.orEmpty()) }
    var never by rememberSaveable { mutableStateOf(existing?.never.orEmpty()) }
    var body by rememberSaveable { mutableStateOf(existing?.body.orEmpty()) }
    var motto by rememberSaveable { mutableStateOf(existing?.motto.orEmpty()) }
    var practice by rememberSaveable { mutableStateOf(existing?.practice.orEmpty()) }
    var anchor by rememberSaveable { mutableStateOf(existing?.anchor.orEmpty()) }
    var model by rememberSaveable { mutableStateOf(existing?.model.orEmpty()) }
    var triggers by rememberSaveable { mutableStateOf(existing?.triggers.orEmpty()) }
    var protects by rememberSaveable { mutableStateOf(existing?.protects.orEmpty()) }
    var needs by rememberSaveable { mutableStateOf(existing?.needs.orEmpty()) }
    var signs by rememberSaveable { mutableStateOf(existing?.signs.orEmpty()) }
    var answer by rememberSaveable { mutableStateOf(existing?.answer.orEmpty()) }
    var sphere by rememberSaveable {
        mutableLongStateOf(existing?.sphereId ?: sphereId.takeIf { it != 0L } ?: if (isShadow) 0L else (w.spheres.firstOrNull()?.id ?: 0L))
    }
    var hue by rememberSaveable { mutableIntStateOf(existing?.hue ?: -1) }
    var antidote by rememberSaveable { mutableLongStateOf(existing?.antidoteId ?: 0L) }
    var pickAntidote by remember { mutableStateOf(false) }

    val sphereRow = w.sphere(sphere)
    val color = when {
        isShadow -> U.c.shadow
        hue >= 0 -> U.c.hue(hue)
        else -> U.c.hue(sphereRow?.hue ?: 0)
    }
    val valid = name.isNotBlank() && (isShadow || sphere != 0L)

    Column(Modifier.fillMaxSize().statusBarsPadding().imePadding()) {
        TopBar(
            stringResource(
                when {
                    existing != null && isShadow -> R.string.rs_rform_edit_shadow
                    existing != null -> R.string.rs_rform_edit_role
                    isShadow -> R.string.rs_rform_new_shadow
                    else -> R.string.rs_rform_new_role
                }
            ),
            onBack = { go.back() },
        )
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = SidePad)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Facet(color, Modifier.size(52.dp), shadow = isShadow)
                Spacer(Modifier.width(14.dp))
                Text(name.ifBlank { stringResource(if (isShadow) R.string.rs_rform_shadow_name_hint else R.string.rs_rform_name_hint) }, style = UType.Title)
            }
            Field(stringResource(R.string.rs_rform_name), name, { name = it }, stringResource(if (isShadow) R.string.rs_rform_shadow_name_hint else R.string.rs_rform_name_hint))
            Field(stringResource(R.string.rs_rform_tagline), tagline, { tagline = it }, gs(if (isShadow) R.string.rs_rform_shadow_tagline_hint else R.string.rs_rform_tagline_hint), lines = 2)
            if (isShadow) {
                Field(stringResource(R.string.rs_shadow_triggers), triggers, { triggers = it }, gs(R.string.rs_rform_triggers_hint), lines = 2)
                Field(stringResource(R.string.rs_shadow_protects), protects, { protects = it }, gs(R.string.rs_rform_protects_hint), lines = 2)
                Field(stringResource(R.string.rs_shadow_needs), needs, { needs = it }, gs(R.string.rs_rform_needs_hint), lines = 2)
                Field(stringResource(R.string.rs_shadow_signs), signs, { signs = it }, gs(R.string.rs_rform_signs_hint), lines = 2)
                Field(stringResource(R.string.rs_shadow_answer), answer, { answer = it }, gs(R.string.rs_rform_answer_hint), lines = 2)
                SectionTitle(stringResource(R.string.rs_shadow_antidote))
                val anti = w.role(antidote)
                if (anti != null) {
                    UCard(padding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) { RoleRow(anti, w, onClick = { pickAntidote = true }) }
                    Spacer(Modifier.height(8.dp))
                    SoftButton(stringResource(R.string.remove), { antidote = 0 }, color = U.c.muted)
                } else {
                    SoftButton(stringResource(R.string.rs_rform_pick_antidote), { pickAntidote = true }, Modifier.fillMaxWidth(), color = U.c.shadow)
                }
            } else {
                Field(stringResource(R.string.rs_role_motto), motto, { motto = it }, gs(R.string.rs_rform_motto_hint))
                Field(stringResource(R.string.rs_role_qualities), qualities, { qualities = it }, gs(R.string.rs_rform_qualities_hint), lines = 3, note = stringResource(R.string.rs_rform_one_per_line))
                Field(stringResource(R.string.rs_role_voice), voice, { voice = it }, gs(R.string.rs_rform_voice_hint), lines = 2)
                Field(stringResource(R.string.rs_role_does), does, { does = it }, gs(R.string.rs_rform_does_hint), lines = 3, note = stringResource(R.string.rs_rform_one_per_line))
                Field(stringResource(R.string.rs_role_never), never, { never = it }, gs(R.string.rs_rform_never_hint), lines = 2, note = stringResource(R.string.rs_rform_one_per_line))
                Field(stringResource(R.string.rs_role_body), body, { body = it }, gs(R.string.rs_rform_body_hint), lines = 2)
                Field(stringResource(R.string.rs_role_practice), practice, { practice = it }, gs(R.string.rs_rform_practice_hint), lines = 2)
                Field(stringResource(R.string.rs_role_anchor), anchor, { anchor = it }, gs(R.string.rs_rform_anchor_hint), lines = 2)
                Field(stringResource(R.string.rs_role_model), model, { model = it }, gs(R.string.rs_rform_model_hint), lines = 2)
            }
            SectionTitle(stringResource(if (isShadow) R.string.rs_rform_sphere_optional else R.string.rs_rform_sphere))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              (if (isShadow) listOf(0L) + w.spheres.map { it.id } else w.spheres.map { it.id }).forEach { sid ->
                val s = w.sphere(sid)
                Chip(
                    if (s == null) stringResource(R.string.rs_rform_no_sphere) else sphereName(s),
                    selected = sphere == sid,
                    onClick = { sphere = sid },
                    color = if (s == null) U.c.shadow else U.c.hue(s.hue),
                    icon = s?.let { sphereIcon(it) },
                )
              }
            }
            if (!isShadow) {
                SectionTitle(stringResource(R.string.rs_rform_color))
                HuePicker(hue, { hue = it }, none = U.c.hue(sphereRow?.hue ?: 0))
                Spacer(Modifier.height(6.dp))
                Text(stringResource(R.string.rs_rform_color_hint), style = UType.Small)
            }
            Spacer(Modifier.height(24.dp))
        }
        PrimaryButton(
            stringResource(R.string.save),
            enabled = valid,
            color = if (isShadow) U.c.shadow else U.c.accent,
            onClick = {
                val base = existing ?: Role(sphereId = sphere, shadow = isShadow, name = "", createdAt = System.currentTimeMillis())
                val sort = if (existing != null && existing.sphereId == sphere) existing.sort
                else (w.roles.filter { it.sphereId == sphere }.maxOfOrNull { it.sort } ?: -1) + 1
                val row = base.copy(
                    sphereId = sphere,
                    name = name.trim(),
                    tagline = tagline.trim(),
                    qualities = qualities.trim(),
                    voice = voice.trim(),
                    does = does.trim(),
                    never = never.trim(),
                    body = body.trim(),
                    motto = motto.trim(),
                    practice = practice.trim(),
                    anchor = anchor.trim(),
                    model = model.trim(),
                    hue = if (isShadow) -1 else hue,
                    triggers = triggers.trim(),
                    protects = protects.trim(),
                    needs = needs.trim(),
                    signs = signs.trim(),
                    answer = answer.trim(),
                    antidoteId = if (isShadow) antidote else 0,
                    sort = sort,
                )
                if (existing != null) {
                    container.write { upsertRole(row) }
                    go.back()
                } else {
                    container.scope.launch {
                        val newId = container.dao.upsertRole(row)
                        container.changed()
                        withContext(Dispatchers.Main) { go.replaceWithRole(newId) }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = SidePad, vertical = 12.dp).navigationBarsPadding(),
        )
    }
    if (pickAntidote) {
        RolePickerDialog(w, stringResource(R.string.rs_rform_pick_antidote), onPick = { antidote = it.id }, onDismiss = { pickAntidote = false })
    }
}

@Composable
private fun Field(title: String, value: String, onChange: (String) -> Unit, hint: String, lines: Int = 1, note: String? = null) {
    SectionTitle(title)
    UTextField(value, onChange, placeholder = hint, minLines = lines)
    if (note != null) {
        Spacer(Modifier.height(4.dp))
        Text(note, style = UType.Small, modifier = Modifier.padding(start = 4.dp))
    }
}
