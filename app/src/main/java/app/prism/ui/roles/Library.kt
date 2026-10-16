package app.prism.ui.roles

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.prism.R
import app.prism.container
import app.prism.content.RoleText
import app.prism.content.ShadowText
import app.prism.content.SpherePresets
import app.prism.data.adopt
import app.prism.data.adoptShadow
import app.prism.domain.gendered
import app.prism.ui.Go
import app.prism.ui.LocalPerson
import app.prism.ui.SphereIcon
import app.prism.ui.WData
import app.prism.ui.components.Chip
import app.prism.ui.components.EmptyState
import app.prism.ui.components.Facet
import app.prism.ui.components.Pill
import app.prism.ui.components.PrimaryButton
import app.prism.ui.components.SectionTitle
import app.prism.ui.components.Segmented
import app.prism.ui.components.TopBar
import app.prism.ui.components.UCard
import app.prism.ui.components.UTextField
import app.prism.ui.g
import app.prism.ui.gs
import app.prism.ui.library
import app.prism.ui.theme.U
import app.prism.ui.theme.UType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun LibraryScreen(sphereKey: String, shadows: Boolean, data: WData, go: Go) {
    val w = data.world
    val lib = library()
    var showShadows by rememberSaveable { mutableStateOf(shadows) }
    var filter by rememberSaveable { mutableStateOf(sphereKey) }
    var query by rememberSaveable { mutableStateOf("") }
    val q = query.trim().lowercase()
    val person = LocalPerson.current
    fun t(s: String) = gendered(s, person.gender).lowercase()
    val roles = lib.roles.filter { r ->
        (filter.isEmpty() || r.sphere == filter) &&
            (q.isEmpty() || t(r.name).contains(q) || t(r.tagline).contains(q) || r.qualities.any { t(it).contains(q) })
    }
    val shadowList = lib.shadows.filter { s -> q.isEmpty() || t(s.name).contains(q) || t(s.tagline).contains(q) }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        TopBar(stringResource(R.string.rs_lib_title), onBack = { go.back() })
        Column(Modifier.padding(horizontal = SidePad)) {
            Segmented(
                listOf(false, true), showShadows,
                label = { stringResource(if (it) R.string.rs_lib_tab_shadows else R.string.rs_lib_tab_roles) },
                onSelect = { showShadows = it },
            )
            Spacer(Modifier.height(12.dp))
            UTextField(query, { query = it }, placeholder = stringResource(R.string.rs_lib_search))
        }
        if (!showShadows) {
            LazyRow(
                Modifier.padding(top = 10.dp),
                contentPadding = PaddingValues(horizontal = SidePad),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item { Chip(stringResource(R.string.rs_lib_all), filter.isEmpty(), { filter = "" }) }
                items(SpherePresets, key = { it.key }) { p ->
                    Chip(
                        g(lib.spheres[p.key]?.name.orEmpty()), filter == p.key, { filter = if (filter == p.key) "" else p.key },
                        color = U.c.hue(p.hue), icon = SphereIcon.of(p.icon).icon,
                    )
                }
            }
        }
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = SidePad, end = SidePad, top = 12.dp, bottom = 32.dp)) {
            if (showShadows) {
                item { Text(gs(R.string.rs_lib_shadows_intro), style = UType.Sub, modifier = Modifier.padding(bottom = 12.dp)) }
                if (shadowList.isEmpty()) item { EmptyState(stringResource(R.string.rs_lib_nothing)) }
                items(shadowList, key = { it.key }) { s ->
                    LibCard(g(s.name), g(s.tagline), null, U.c.shadow, w.hasPreset(s.key), shadow = true) { go.preset(s.key) }
                    Spacer(Modifier.height(10.dp))
                }
            } else {
                if (roles.isEmpty()) item { EmptyState(stringResource(R.string.rs_lib_nothing)) }
                items(roles, key = { it.key }) { r ->
                    val preset = SpherePresets.firstOrNull { it.key == r.sphere }
                    val color = U.c.hue(preset?.hue ?: 0)
                    LibCard(g(r.name), g(r.tagline), g(lib.spheres[r.sphere]?.name.orEmpty()), color, w.hasPreset(r.key)) { go.preset(r.key) }
                    Spacer(Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
private fun LibCard(name: String, tagline: String, sphere: String?, color: Color, added: Boolean, shadow: Boolean = false, onClick: () -> Unit) {
    UCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.Top) {
            Facet(color, Modifier.size(34.dp), shadow = shadow)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(name, style = UType.Strong)
                Spacer(Modifier.height(2.dp))
                Text(tagline, style = UType.Muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (sphere != null || added) {
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (sphere != null) Pill(sphere, color)
                        if (added) Pill(stringResource(R.string.rs_lib_added), U.c.good, icon = Icons.Rounded.Check)
                    }
                }
            }
        }
    }
}

@Composable
fun PresetScreen(key: String, data: WData, go: Go) {
    val w = data.world
    val lib = library()
    val person = LocalPerson.current
    val context = LocalContext.current
    val container = context.container
    val mine = (w.roles + w.shadows).firstOrNull { it.presetKey == key }
    val fix: (String) -> String = { gendered(it, person.gender) }
    val role = if (key.startsWith("shadow.")) null else lib.role(key)
    val shadow = if (key.startsWith("shadow.")) lib.shadow(key) else null
    if (role == null && shadow == null) return

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        TopBar(stringResource(R.string.rs_lib_title), onBack = { go.back() })
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(start = SidePad, end = SidePad, bottom = 20.dp)) {
            item {
                if (role != null) RolePreview(role) else if (shadow != null) ShadowPreview(shadow)
            }
        }
        PrimaryButton(
            stringResource(
                when {
                    mine != null -> R.string.rs_lib_open_mine
                    shadow != null -> R.string.rs_lib_add_shadow
                    else -> R.string.rs_lib_add_role
                }
            ),
            color = if (shadow != null) U.c.shadow else U.c.accent,
            icon = if (mine != null) null else Icons.Rounded.Add,
            onClick = {
                if (mine != null) go.role(mine.id)
                else container.scope.launch {
                    val now = System.currentTimeMillis()
                    val id = if (role != null) container.dao.adopt(role, now, fix) else container.dao.adoptShadow(shadow!!, now, fix)
                    container.changed()
                    withContext(Dispatchers.Main) { go.replaceWithRole(id) }
                }
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = SidePad, vertical = 12.dp).navigationBarsPadding(),
        )
    }
}

@Composable
private fun PreviewHero(name: String, tagline: String, eyebrow: String, color: Color, shadow: Boolean, motto: String = "") {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(U.RXl)
            .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(color.copy(alpha = 0.3f), color.copy(alpha = 0.05f))))
            .border(1.dp, color.copy(alpha = 0.4f), U.RXl)
            .padding(22.dp),
    ) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Facet(color, Modifier.size(72.dp), shadow = shadow)
            Spacer(Modifier.height(12.dp))
            if (eyebrow.isNotBlank()) Text(eyebrow.uppercase(), style = UType.Eyebrow.copy(color = color))
            Spacer(Modifier.height(4.dp))
            Text(name, style = UType.Title.copy(textAlign = TextAlign.Center))
            Spacer(Modifier.height(6.dp))
            Text(tagline, style = UType.Sub.copy(textAlign = TextAlign.Center))
            if (motto.isNotBlank()) {
                Spacer(Modifier.height(14.dp))
                Text("«$motto»", style = UType.Motto.copy(textAlign = TextAlign.Center))
            }
        }
    }
}

@Composable
private fun RolePreview(t: RoleText) {
    val lib = library()
    val color = U.c.hue(SpherePresets.firstOrNull { it.key == t.sphere }?.hue ?: 0)
    PreviewHero(g(t.name), g(t.tagline), g(lib.spheres[t.sphere]?.name.orEmpty()), color, shadow = false, motto = g(t.motto))
    SectionTitle(stringResource(R.string.rs_role_qualities))
    QualityChips(t.qualities.map { g(it) }, color)
    TextSection(stringResource(R.string.rs_role_voice), g(t.voice))
    ListSection(stringResource(R.string.rs_role_does), t.does.map { g(it) }, Icons.Rounded.Check, color)
    ListSection(stringResource(R.string.rs_role_never), t.never.map { g(it) }, Icons.Rounded.Close, U.c.danger)
    TextSection(stringResource(R.string.rs_role_body), g(t.body))
    TextSection(stringResource(R.string.rs_role_practice), g(t.practice))
    SectionTitle(stringResource(R.string.rs_lib_act_tasks))
    UCard {
        Text(gs(R.string.rs_lib_act_tasks_sub), style = UType.Muted)
        t.tasks.forEachIndexed { i, task ->
            Spacer(Modifier.height(10.dp))
            Row {
                Text("${i + 1}", style = UType.Strong.copy(color = color), modifier = Modifier.width(22.dp))
                Text(g(task), style = UType.Body, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ShadowPreview(s: ShadowText) {
    val lib = library()
    PreviewHero(g(s.name), g(s.tagline), stringResource(R.string.rs_lib_tab_shadows), U.c.shadow, shadow = true)
    TextSection(stringResource(R.string.rs_shadow_triggers), g(s.triggers))
    TextSection(stringResource(R.string.rs_shadow_protects), g(s.protects))
    TextSection(stringResource(R.string.rs_shadow_needs), g(s.needs))
    TextSection(stringResource(R.string.rs_shadow_signs), g(s.signs))
    TextSection(stringResource(R.string.rs_shadow_answer), g(s.answer))
    if (s.answerSpheres.isNotEmpty()) {
        SectionTitle(stringResource(R.string.rs_lib_answer_spheres))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            s.answerSpheres.forEach { key ->
                val p = SpherePresets.firstOrNull { it.key == key }
                Pill(g(lib.spheres[key]?.name.orEmpty()), U.c.hue(p?.hue ?: 0), icon = p?.let { SphereIcon.of(it.icon).icon })
            }
        }
    }
}
