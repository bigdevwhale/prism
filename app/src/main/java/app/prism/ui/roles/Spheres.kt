package app.prism.ui.roles

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.prism.R
import app.prism.container
import app.prism.content.RoleText
import app.prism.data.Sphere
import app.prism.data.adopt
import app.prism.data.deleteSphere
import app.prism.domain.gendered
import app.prism.ui.Go
import app.prism.ui.LocalPerson
import app.prism.ui.SphereIcon
import app.prism.ui.WData
import app.prism.ui.components.ConfirmDialog
import app.prism.ui.components.IconDisc
import app.prism.ui.components.PrimaryButton
import app.prism.ui.components.RoleRow
import app.prism.ui.components.SectionTitle
import app.prism.ui.components.SoftButton
import app.prism.ui.components.TopBar
import app.prism.ui.components.UCard
import app.prism.ui.components.UTextField
import app.prism.ui.g
import app.prism.ui.gp
import app.prism.ui.gs
import app.prism.ui.library
import app.prism.ui.sphereBlurb
import app.prism.ui.sphereIcon
import app.prism.ui.sphereName
import app.prism.ui.theme.U
import app.prism.ui.theme.UType

@Composable
fun SphereScreen(id: Long, data: WData, go: Go) {
    val w = data.world
    val s = w.sphere(id) ?: return
    val context = LocalContext.current
    val gender = LocalPerson.current.gender
    val lib = library()
    val hue = U.c.hue(s.hue)
    val roles = w.rolesIn(id)
    val shadows = w.shadowsIn(id)
    val suggestions = s.presetKey?.let { key -> lib.rolesOf(key).filter { !w.hasPreset(it.key) } }.orEmpty()
    var browse by rememberSaveable { mutableStateOf(false) }
    val adopt: (RoleText, Long?) -> Unit = { t, sphereId ->
        val now = System.currentTimeMillis()
        context.container.write { adopt(t, now, fix = { gendered(it, gender) }, sphereId = sphereId) }
    }
    val from = data.day - 29
    val recent = w.scenes.filter { sc -> sc.day >= from && w.role(sc.roleId)?.let { it.sphereId == id && !it.shadow } == true }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        TopBar("", onBack = { go.back() }) {
            IconButton(onClick = { go.sphereForm(id) }) { Icon(Icons.Rounded.Edit, stringResource(R.string.edit), tint = U.c.ink) }
        }
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = SidePad, end = SidePad, bottom = 32.dp)) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconDisc(sphereIcon(s), 56.dp, hue)
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(sphereName(s), style = UType.Title)
                        val blurb = sphereBlurb(s)
                        if (blurb.isNotBlank()) Text(blurb, style = UType.Sub)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    if (recent.isEmpty()) gs(R.string.rs_sphere_no_scenes) else gp(R.plurals.rs_sphere_scenes_30, recent.size, recent.size),
                    style = UType.Muted,
                )
            }
            item { SectionTitle(stringResource(R.string.rs_sphere_my_roles)) }
            item {
                UCard(padding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)) {
                    if (roles.isEmpty()) Text(gs(R.string.rs_sphere_roles_empty), style = UType.Sub, modifier = Modifier.padding(vertical = 10.dp))
                    roles.forEachIndexed { i, r ->
                        if (i > 0) HorizontalDivider(color = U.c.line)
                        RoleRow(r, w, showSphere = false, onClick = { go.role(r.id) })
                    }
                }
                Spacer(Modifier.height(10.dp))
                SoftButton(stringResource(R.string.rs_sphere_write_role), { go.roleForm(sphereId = id) }, Modifier.fillMaxWidth(), color = hue, icon = Icons.Rounded.Edit)
            }
            if (suggestions.isNotEmpty()) {
                item { SectionTitle(stringResource(R.string.rs_sphere_suggestions)) }
                items(suggestions, key = { it.key }) { t ->
                    SuggestionCard(t, hue, onOpen = { go.preset(t.key) }, onAdd = { adopt(t, id) })
                    Spacer(Modifier.height(10.dp))
                }
            }
            if (s.presetKey == null) {
                item {
                    Spacer(Modifier.height(14.dp))
                    SoftButton(
                        stringResource(if (browse) R.string.rs_sphere_hide_library else R.string.rs_sphere_browse_library),
                        { browse = !browse }, Modifier.fillMaxWidth(), color = hue,
                    )
                }
                if (browse) {
                    lib.spheres.keys.forEach { key ->
                        val list = lib.rolesOf(key).filter { !w.hasPreset(it.key) }
                        if (list.isNotEmpty()) {
                            item(key = "h$key") { SectionTitle(g(lib.spheres[key]?.name.orEmpty())) }
                            items(list, key = { "l" + it.key }) { t ->
                                SuggestionCard(t, hue, onOpen = { go.preset(t.key) }, onAdd = { adopt(t, id) })
                                Spacer(Modifier.height(10.dp))
                            }
                        }
                    }
                }
            }
            if (shadows.isNotEmpty()) {
                item { SectionTitle(stringResource(R.string.rs_roles_shadows)) }
                item {
                    UCard(padding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)) {
                        shadows.forEachIndexed { i, sh ->
                            if (i > 0) HorizontalDivider(color = U.c.line)
                            RoleRow(sh, w, showSphere = false, onClick = { go.role(sh.id) })
                        }
                    }
                }
            }
        }
    }
}

/** A library role offered for adoption: tap to preview, the button adds it right away. */
@Composable
internal fun SuggestionCard(t: RoleText, color: Color, onOpen: () -> Unit, onAdd: () -> Unit) {
    UCard(onClick = onOpen) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(g(t.name), style = UType.Strong)
                Spacer(Modifier.height(2.dp))
                Text(g(t.tagline), style = UType.Muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(10.dp))
            Box(
                Modifier.size(40.dp).clip(U.RSm).background(color.copy(alpha = 0.14f)).clickable(onClick = onAdd),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.Add, stringResource(R.string.add), tint = color) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SphereFormScreen(id: Long, data: WData, go: Go) {
    val w = data.world
    val existing = if (id != 0L) w.sphere(id) else null
    if (id != 0L && existing == null) return
    val context = LocalContext.current
    var name by rememberSaveable { mutableStateOf(existing?.name.orEmpty()) }
    var icon by rememberSaveable { mutableStateOf(existing?.icon ?: SphereIcon.STAR.name) }
    var hue by rememberSaveable { mutableIntStateOf(existing?.hue ?: (w.spheres.size % 12)) }
    var confirmDelete by remember { mutableStateOf(false) }
    val preset = existing?.presetKey
    val defaultName = preset?.let { g(library().spheres[it]?.name.orEmpty()) }.orEmpty()
    val valid = preset != null || name.isNotBlank()
    val color = U.c.hue(hue)

    Column(Modifier.fillMaxSize().statusBarsPadding().imePadding()) {
        TopBar(stringResource(if (existing == null) R.string.rs_sphere_new else R.string.rs_sphere_edit), onBack = { go.back() }) {
            if (existing != null) IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Rounded.Delete, stringResource(R.string.delete), tint = U.c.danger) }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = SidePad)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconDisc(SphereIcon.of(icon).icon, 56.dp, color)
                Spacer(Modifier.width(16.dp))
                Text(name.ifBlank { defaultName.ifBlank { stringResource(R.string.rs_sphere_name_hint) } }, style = UType.Title, modifier = Modifier.weight(1f))
            }
            SectionTitle(stringResource(R.string.rs_sphere_name))
            UTextField(name, { name = it }, placeholder = defaultName.ifBlank { stringResource(R.string.rs_sphere_name_hint) })
            if (preset != null) {
                Spacer(Modifier.height(6.dp))
                Text(stringResource(R.string.rs_sphere_name_preset), style = UType.Small)
            }
            SectionTitle(stringResource(R.string.rs_sphere_icon))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SphereIcon.entries.forEach { e ->
                    val on = e.name == icon
                    Box(
                        Modifier
                            .size(46.dp)
                            .clip(U.RSm)
                            .background(if (on) color.copy(alpha = 0.18f) else U.c.surface)
                            .border(1.dp, if (on) color else U.c.line, U.RSm)
                            .clickable { icon = e.name },
                        contentAlignment = Alignment.Center,
                    ) { Icon(e.icon, null, Modifier.size(22.dp), tint = if (on) color else U.c.muted) }
                }
            }
            SectionTitle(stringResource(R.string.rs_sphere_color))
            HuePicker(hue, { hue = it })
            Spacer(Modifier.height(24.dp))
        }
        PrimaryButton(
            stringResource(R.string.save),
            onClick = {
                val sort = existing?.sort ?: ((w.spheres.maxOfOrNull { it.sort } ?: -1) + 1)
                val row = (existing ?: Sphere(icon = icon, hue = hue)).copy(name = name.trim(), icon = icon, hue = hue, sort = sort)
                context.container.write { upsertSphere(row) }
                go.back()
            },
            enabled = valid,
            modifier = Modifier.fillMaxWidth().padding(horizontal = SidePad, vertical = 12.dp).navigationBarsPadding(),
        )
    }
    if (confirmDelete && existing != null) {
        ConfirmDialog(
            title = stringResource(R.string.rs_sphere_delete_title),
            text = gs(R.string.rs_sphere_delete_text),
            confirm = stringResource(R.string.delete),
            danger = true,
            onConfirm = {
                context.container.write { deleteSphere(existing.id) }
                go.home()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}
