package app.prism.ui.practices

import androidx.compose.foundation.layout.Arrangement
import app.prism.ui.quoted
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.prism.R
import app.prism.container
import app.prism.data.Note
import app.prism.domain.Practice
import app.prism.domain.World
import app.prism.ui.Go
import app.prism.ui.WData
import app.prism.ui.components.Chip
import app.prism.ui.components.ConfirmDialog
import app.prism.ui.components.Facet
import app.prism.ui.components.PrimaryButton
import app.prism.ui.components.SectionTitle
import app.prism.ui.components.UCard
import app.prism.ui.components.UTextField
import app.prism.ui.components.roleColor
import app.prism.ui.gs
import app.prism.ui.roleName
import app.prism.ui.theme.U
import app.prism.ui.theme.UType
import org.json.JSONArray
import org.json.JSONObject

private class Council(val question: String, val answers: List<Pair<Long, String>>, val core: String, val decision: String)

private fun parseCouncil(n: Note): Council {
    val o = json(n.body)
    val a = o.optJSONArray("answers") ?: JSONArray()
    val answers = List(a.length()) { i -> a.optJSONObject(i)?.let { it.optLong("roleId") to it.optString("text", "") } ?: (0L to "") }
    return Council(n.title, answers, o.str("core"), o.str("decision"))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CouncilScreen(id: Long, data: WData, go: Go) {
    val w = data.world
    val context = LocalContext.current
    val existing = w.notes.firstOrNull { it.id == id && it.kind == Practice.COUNCIL.name }
    val parsed = existing?.let { parseCouncil(it) }
    var editing by rememberSaveable(id) { mutableStateOf(existing == null) }
    var confirmDelete by remember { mutableStateOf(false) }

    var question by rememberSaveable(id) { mutableStateOf(parsed?.question.orEmpty()) }
    val chosen = remember(id) { mutableStateListOf<Long>().apply { parsed?.answers?.forEach { add(it.first) } } }
    val answers = remember(id) { mutableStateMapOf<Long, String>().apply { parsed?.answers?.forEach { put(it.first, it.second) } } }
    var core by rememberSaveable(id) { mutableStateOf(parsed?.core.orEmpty()) }
    var decision by rememberSaveable(id) { mutableStateOf(parsed?.decision.orEmpty()) }

    val shareTitle = gs(R.string.pr_council)
    val coreLabel = gs(R.string.cnc_core)
    val decisionLabel = gs(R.string.cnc_decision)

    PracticeScaffold(
        gs(R.string.pr_council),
        { go.back() },
        actions = {
            if (existing != null && !editing && parsed != null) {
                IconButton(onClick = {
                    shareText(context, shareTitle, councilText(parsed, w, coreLabel, decisionLabel))
                }) { Icon(Icons.Rounded.Share, gs(R.string.share), tint = U.c.muted) }
                IconButton(onClick = { editing = true }) { Icon(Icons.Rounded.Edit, gs(R.string.edit), tint = U.c.muted) }
                IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Rounded.DeleteOutline, gs(R.string.delete), tint = U.c.muted) }
            }
        },
    ) {
        if (!editing && parsed != null) {
            item {
                Text(parsed.question, style = UType.Title, modifier = Modifier.padding(top = 4.dp, bottom = 10.dp))
            }
            parsed.answers.forEach { (rid, text) ->
                item { AnswerCard(w, rid, text) }
            }
            if (parsed.core.isNotBlank()) item {
                SectionTitle(coreLabel)
                UCard(border = U.c.accent.copy(alpha = 0.4f)) { Text(parsed.core, style = UType.Body) }
            }
            if (parsed.decision.isNotBlank()) item {
                SectionTitle(decisionLabel)
                Text(parsed.decision, style = UType.Motto, modifier = Modifier.padding(horizontal = 4.dp))
            }
            return@PracticeScaffold
        }

        item {
            Text(gs(R.string.cnc_intro), style = UType.Sub, modifier = Modifier.padding(start = 2.dp, bottom = 4.dp))
            FieldLabel(gs(R.string.cnc_question))
            UTextField(question, { question = it }, placeholder = gs(R.string.cnc_question_hint), minLines = 2)
            FieldLabel(gs(R.string.cnc_pick, 2, 5))
            if (w.roles.isEmpty()) Text(gs(R.string.picker_no_roles), style = UType.Sub)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                w.roles.forEach { r ->
                    val on = r.id in chosen
                    Chip(roleName(r), on, {
                        if (on) chosen.remove(r.id) else if (chosen.size < 5) chosen.add(r.id)
                    }, color = roleColor(w, r))
                }
            }
        }
        chosen.toList().forEach { rid ->
            val r = w.role(rid) ?: return@forEach
            item(key = "ans$rid") {
                val color = roleColor(w, r)
                Spacer(Modifier.height(12.dp))
                UCard(border = color.copy(alpha = 0.45f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Facet(color, Modifier.size(30.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(gs(R.string.cnc_says, roleName(r)), style = UType.Strong)
                            if (r.motto.isNotBlank()) Text(quoted(r.motto), style = UType.Muted.copy(color = color))
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    UTextField(answers[rid].orEmpty(), { answers[rid] = it }, placeholder = gs(R.string.cnc_says_hint), minLines = 2)
                }
            }
        }
        item {
            SectionTitle(gs(R.string.cnc_core_q))
            if (w.core.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    w.core.forEach { v ->
                        Chip(v.text, core.contains(v.text, ignoreCase = true), {
                            core = if (core.isBlank()) v.text else core.trimEnd() + ", " + v.text
                        })
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            UTextField(core, { core = it }, placeholder = gs(R.string.cnc_core_hint), minLines = 2)
            FieldLabel(decisionLabel)
            UTextField(decision, { decision = it }, placeholder = gs(R.string.cnc_decision_hint), minLines = 2)
            Spacer(Modifier.height(20.dp))
            PrimaryButton(
                gs(R.string.save),
                {
                    val body = JSONObject()
                        .put("answers", JSONArray().apply { chosen.forEach { put(JSONObject().put("roleId", it).put("text", answers[it].orEmpty().trim())) } })
                        .put("core", core.trim())
                        .put("decision", decision.trim())
                        .toString()
                    val note = (existing ?: Note(kind = Practice.COUNCIL.name, day = data.day, createdAt = data.now)).copy(title = question.trim(), body = body)
                    context.container.write { upsertNote(note) }
                    if (existing != null) editing = false else go.back()
                },
                Modifier.fillMaxWidth(),
                enabled = question.isNotBlank() && chosen.size >= 2,
            )
        }
    }

    if (confirmDelete && existing != null) ConfirmDialog(
        gs(R.string.note_delete_title), gs(R.string.note_delete_text), gs(R.string.delete),
        onConfirm = { context.container.write { deleteNote(existing.id) }; go.back() },
        onDismiss = { confirmDelete = false }, danger = true,
    )
}

@Composable
private fun AnswerCard(w: World, rid: Long, text: String) {
    val r = w.role(rid)
    val color = roleColor(w, r)
    UCard(Modifier.padding(bottom = 10.dp), border = color.copy(alpha = 0.45f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Facet(color, Modifier.size(26.dp))
            Spacer(Modifier.width(10.dp))
            Text(roleName(r).ifBlank { "—" }, style = UType.Strong.copy(color = color))
        }
        if (text.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(text, style = UType.Body)
        }
    }
}

private fun councilText(c: Council, w: World, coreLabel: String, decisionLabel: String): String = buildString {
    appendLine(c.question)
    appendLine()
    c.answers.forEach { (rid, t) -> appendLine("${w.role(rid)?.name ?: "—"}: $t") }
    if (c.core.isNotBlank()) { appendLine(); appendLine("$coreLabel: ${c.core}") }
    if (c.decision.isNotBlank()) { appendLine(); appendLine("$decisionLabel: ${c.decision}") }
}.trim()
