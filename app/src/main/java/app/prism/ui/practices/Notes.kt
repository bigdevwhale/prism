package app.prism.ui.practices

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.unit.dp
import app.prism.R
import app.prism.container
import app.prism.data.Note
import app.prism.domain.Practice
import app.prism.ui.Go
import app.prism.ui.WData
import app.prism.ui.components.ConfirmDialog
import app.prism.ui.components.Facet
import app.prism.ui.components.PrimaryButton
import app.prism.ui.components.RolePickerDialog
import app.prism.ui.components.SectionTitle
import app.prism.ui.components.SoftButton
import app.prism.ui.components.UCard
import app.prism.ui.components.UTextField
import app.prism.ui.components.practiceBlurb
import app.prism.ui.components.practiceName
import app.prism.ui.components.roleColor
import app.prism.ui.g
import app.prism.ui.gs
import app.prism.ui.roleName
import app.prism.ui.theme.U
import app.prism.ui.theme.UType
import org.json.JSONArray
import org.json.JSONObject

/** A who-said-what row of "Through their eyes" and "Speech at eighty". */
private data class Voice(val who: String, val text: String)

@Composable
fun NoteScreen(kind: Practice, id: Long, roleId: Long, data: WData, go: Go) {
    val w = data.world
    val context = LocalContext.current
    val existing = w.notes.firstOrNull { it.id == id && it.kind == kind.name }
    val body = remember(existing?.body) { json(existing?.body.orEmpty()) }

    var editing by rememberSaveable(id) { mutableStateOf(existing == null) }
    var rid by rememberSaveable(id) { mutableLongStateOf(existing?.roleId ?: roleId) }
    var pickRole by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    // Fields of every kind; each kind uses its own.
    var text by rememberSaveable(id) { mutableStateOf(body.str("text")) }
    var name by rememberSaveable(id) { mutableStateOf(if (kind == Practice.SHADOW_TALK) existing?.title.orEmpty() else "") }
    val answers = remember(id) { mutableStateListOf<String>().apply { addAll(body.strings("answers").let { a -> List(5) { a.getOrElse(it) { "" } } }) } }
    val rows = remember(id) {
        mutableStateListOf<Voice>().apply {
            val a = body.optJSONArray("rows") ?: JSONArray()
            for (i in 0 until a.length()) a.optJSONObject(i)?.let { add(Voice(it.optString("who", ""), it.optString("text", ""))) }
            if (isEmpty()) add(Voice("", ""))
        }
    }
    var who by rememberSaveable(id) { mutableStateOf(body.str("who")) }
    var does by rememberSaveable(id) { mutableStateOf(body.str("does")) }
    var borrow by rememberSaveable(id) { mutableStateOf(body.str("borrow")) }

    val role = w.role(rid)
    val needsRole = kind == Practice.LETTER || kind == Practice.MODEL
    LaunchedEffect(Unit) { if (existing == null && needsRole && role == null) pickRole = true }
    val color = if (kind == Practice.SHADOW_TALK) U.c.shadow else if (role != null) roleColor(w, role) else U.c.accent

    val letterTitle = gs(R.string.note_letter_title, roleName(role))
    val title = when (kind) {
        Practice.LETTER -> letterTitle
        Practice.SHADOW_TALK -> name.trim().ifBlank { roleName(role) }
        Practice.MODEL -> who.trim()
        else -> practiceName(kind)
    }
    val suggestions = stringArrayResource(R.array.note_who_suggestions).toList()
    val shadowQs = stringArrayResource(R.array.shadow_talk_questions).map { g(it) }
    val modelLabels = listOf(gs(R.string.model_who), gs(R.string.model_does), gs(R.string.model_borrow))

    fun build(): String = when (kind) {
        Practice.LETTER -> JSONObject().put("text", text.trim())
        Practice.OTHERS, Practice.SPEECH -> JSONObject().put(
            "rows",
            JSONArray().apply { rows.filter { it.who.isNotBlank() || it.text.isNotBlank() }.forEach { put(JSONObject().put("who", it.who.trim()).put("text", it.text.trim())) } },
        )
        Practice.SHADOW_TALK -> JSONObject().put("answers", stringsArray(answers.map { it.trim() }))
        else -> JSONObject().put("who", who.trim()).put("does", does.trim()).put("borrow", borrow.trim())
    }.toString()

    fun plain(): String = buildString {
        appendLine(title)
        appendLine()
        when (kind) {
            Practice.LETTER -> append(text.trim())
            Practice.OTHERS, Practice.SPEECH -> rows.filter { it.text.isNotBlank() }.forEach { appendLine("${it.who}: ${it.text}"); appendLine() }
            Practice.SHADOW_TALK -> shadowQs.forEachIndexed { i, q -> if (answers[i].isNotBlank()) { appendLine(q); appendLine(answers[i]); appendLine() } }
            else -> listOf(who, does, borrow).forEachIndexed { i, v -> if (v.isNotBlank()) appendLine("${modelLabels[i]}: $v") }
        }
    }.trim()

    val canSave = when (kind) {
        Practice.LETTER -> rid != 0L && text.isNotBlank()
        Practice.OTHERS, Practice.SPEECH -> rows.any { it.text.isNotBlank() }
        Practice.SHADOW_TALK -> (name.isNotBlank() || role != null) && answers.any { it.isNotBlank() }
        else -> rid != 0L && who.isNotBlank()
    }

    PracticeScaffold(
        practiceName(kind),
        { go.back() },
        actions = {
            if (existing != null && !editing) {
                IconButton(onClick = { shareText(context, title, plain()) }) { Icon(Icons.Rounded.Share, gs(R.string.share), tint = U.c.muted) }
                IconButton(onClick = { editing = true }) { Icon(Icons.Rounded.Edit, gs(R.string.edit), tint = U.c.muted) }
                IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Rounded.DeleteOutline, gs(R.string.delete), tint = U.c.muted) }
            }
        },
    ) {
        if (!editing) {
            item {
                if (role != null) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 10.dp)) {
                        Facet(color, Modifier.size(26.dp), shadow = role.shadow)
                        Spacer(Modifier.width(10.dp))
                        Text(roleName(role), style = UType.Strong.copy(color = color))
                    }
                }
                Text(title, style = UType.Title, modifier = Modifier.padding(bottom = 14.dp))
            }
            when (kind) {
                Practice.LETTER -> item { UCard(border = color.copy(alpha = 0.4f)) { Text(text, style = UType.Body) } }
                Practice.OTHERS, Practice.SPEECH -> rows.filter { it.text.isNotBlank() }.forEach { r ->
                    item {
                        UCard(Modifier.padding(bottom = 10.dp)) {
                            if (r.who.isNotBlank()) Text(r.who, style = UType.Eyebrow)
                            Spacer(Modifier.height(6.dp))
                            Text(r.text, style = UType.Body)
                        }
                    }
                }
                Practice.SHADOW_TALK -> shadowQs.forEachIndexed { i, q ->
                    if (answers[i].isNotBlank()) item {
                        UCard(Modifier.padding(bottom = 10.dp), border = color.copy(alpha = 0.35f)) {
                            Text(q, style = UType.Muted)
                            Spacer(Modifier.height(6.dp))
                            Text(answers[i], style = UType.Body)
                        }
                    }
                }
                else -> {
                    listOf(who, does, borrow).forEachIndexed { i, v ->
                        if (v.isNotBlank()) item {
                            UCard(Modifier.padding(bottom = 10.dp)) {
                                Text(modelLabels[i], style = UType.Muted)
                                Spacer(Modifier.height(6.dp))
                                Text(v, style = if (i == 2) UType.Motto.copy(color = color) else UType.Body)
                            }
                        }
                    }
                    if (role != null) item {
                        Spacer(Modifier.height(8.dp))
                        if (role.model == who.trim()) Text(gs(R.string.model_saved), style = UType.Muted)
                        else SoftButton(gs(R.string.model_save, roleName(role)), {
                            val r = role.copy(model = who.trim())
                            context.container.write { upsertRole(r) }
                        }, Modifier.fillMaxWidth(), color = color)
                    }
                }
            }
            return@PracticeScaffold
        }

        item {
            Text(g(practiceBlurb(kind)), style = UType.Sub, modifier = Modifier.padding(start = 2.dp, bottom = 4.dp))
            if (needsRole || kind == Practice.SHADOW_TALK) {
                FieldLabel(gs(if (kind == Practice.SHADOW_TALK) R.string.note_shadow else R.string.reh_role))
                UCard(onClick = { pickRole = true }, padding = PaddingValues(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Facet(color, Modifier.size(28.dp), shadow = kind == Practice.SHADOW_TALK)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            if (role != null) roleName(role) else gs(if (kind == Practice.SHADOW_TALK) R.string.pick_shadow else R.string.pick_role),
                            style = UType.Strong,
                        )
                    }
                }
                if (kind == Practice.SHADOW_TALK && role == null) {
                    FieldLabel(gs(R.string.note_shadow_name))
                    UTextField(name, { name = it }, placeholder = gs(R.string.note_shadow_name_hint))
                }
            }
        }
        when (kind) {
            Practice.LETTER -> item {
                SectionTitle(gs(R.string.note_letter_prompts))
                stringArrayResource(R.array.letter_prompts).forEach { Text("·  " + g(it), style = UType.Muted, modifier = Modifier.padding(vertical = 3.dp)) }
                Spacer(Modifier.height(14.dp))
                UTextField(text, { text = it }, placeholder = gs(R.string.note_letter_hint, roleName(role)), minLines = 10)
            }
            Practice.OTHERS, Practice.SPEECH -> {
                item {
                    SectionTitle(gs(if (kind == Practice.OTHERS) R.string.note_others_q else R.string.note_speech_q))
                }
                rows.forEachIndexed { i, r ->
                    item(key = "row$i") {
                        UCard(Modifier.padding(bottom = 10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                UTextField(r.who, { rows[i] = r.copy(who = it) }, Modifier.weight(1f), placeholder = gs(R.string.note_who))
                                if (rows.size > 1) IconButton(onClick = { rows.removeAt(i) }) { Icon(Icons.Rounded.Close, gs(R.string.remove), tint = U.c.faint) }
                            }
                            if (r.who.isBlank()) {
                                Spacer(Modifier.height(8.dp))
                                Suggestions(suggestions.filter { s -> rows.none { it.who == s } }.take(6)) { rows[i] = r.copy(who = it) }
                            }
                            Spacer(Modifier.height(8.dp))
                            UTextField(
                                r.text, { rows[i] = r.copy(text = it) },
                                placeholder = gs(if (kind == Practice.OTHERS) R.string.note_others_hint else R.string.note_speech_hint),
                                minLines = 3,
                            )
                        }
                    }
                }
                item { SoftButton(gs(R.string.note_add_person), { rows.add(Voice("", "")) }, Modifier.fillMaxWidth(), icon = Icons.Rounded.Add) }
            }
            Practice.SHADOW_TALK -> shadowQs.forEachIndexed { i, q ->
                item {
                    FieldLabel(q)
                    UTextField(answers[i], { answers[i] = it }, minLines = 2)
                }
            }
            else -> item {
                FieldLabel(modelLabels[0])
                UTextField(who, { who = it }, placeholder = gs(R.string.model_who_hint))
                FieldLabel(modelLabels[1])
                UTextField(does, { does = it }, placeholder = gs(R.string.model_does_hint), minLines = 3)
                FieldLabel(modelLabels[2])
                UTextField(borrow, { borrow = it }, placeholder = gs(R.string.model_borrow_hint), minLines = 2)
            }
        }
        item {
            Spacer(Modifier.height(20.dp))
            PrimaryButton(
                gs(R.string.save),
                {
                    val note = (existing ?: Note(kind = kind.name, day = data.day, createdAt = data.now)).copy(roleId = rid, title = title, body = build())
                    context.container.write { upsertNote(note) }
                    if (existing != null) editing = false else go.back()
                },
                Modifier.fillMaxWidth(),
                enabled = canSave,
                color = if (kind == Practice.SHADOW_TALK) U.c.accent else color,
            )
        }
    }

    if (pickRole) RolePickerDialog(
        w,
        gs(if (kind == Practice.SHADOW_TALK) R.string.pick_shadow else R.string.pick_role),
        shadows = kind == Practice.SHADOW_TALK,
        onPick = { rid = it.id; if (kind == Practice.SHADOW_TALK) name = "" },
        onDismiss = { pickRole = false },
    )
    if (confirmDelete && existing != null) ConfirmDialog(
        gs(R.string.note_delete_title), gs(R.string.note_delete_text), gs(R.string.delete),
        onConfirm = { context.container.write { deleteNote(existing.id) }; go.back() },
        onDismiss = { confirmDelete = false }, danger = true,
    )
}
