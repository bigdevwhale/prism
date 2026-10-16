package app.prism.ui.roles

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.LibraryBooks
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.prism.R
import app.prism.data.Sphere
import app.prism.ui.Go
import app.prism.ui.WData
import app.prism.ui.components.Facet
import app.prism.ui.components.IconDisc
import app.prism.ui.components.RoleRow
import app.prism.ui.components.SectionTitle
import app.prism.ui.components.SoftButton
import app.prism.ui.components.UCard
import app.prism.ui.components.roleColor
import app.prism.ui.gp
import app.prism.ui.gs
import app.prism.ui.roleName
import app.prism.ui.sphereIcon
import app.prism.ui.sphereName
import app.prism.ui.theme.U
import app.prism.ui.theme.UType

@Composable
fun RolesTab(data: WData, go: Go) {
    val w = data.world
    LazyColumn(Modifier.fillMaxSize().statusBarsPadding(), contentPadding = PaddingValues(start = SidePad, end = SidePad, bottom = 28.dp)) {
        item {
            Row(Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.rs_roles_title), style = UType.Display, modifier = Modifier.weight(1f))
                IconButton(onClick = { go.library() }) {
                    Icon(Icons.AutoMirrored.Rounded.LibraryBooks, stringResource(R.string.rs_roles_library), tint = U.c.ink)
                }
            }
        }
        item { CoreCard(data, go) }
        item { SectionTitle(stringResource(R.string.rs_roles_spheres), action = stringResource(R.string.rs_roles_library), onAction = { go.library() }) }
        items(w.spheres, key = { "s" + it.id }) { s ->
            SphereCard(s, data, go)
            Spacer(Modifier.height(10.dp))
        }
        item {
            SoftButton(stringResource(R.string.rs_roles_new_sphere), { go.sphereForm(0) }, Modifier.fillMaxWidth(), icon = Icons.Rounded.Add)
        }
        item { SectionTitle(stringResource(R.string.rs_roles_shadows)) }
        item {
            UCard(padding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)) {
                if (w.shadows.isEmpty()) {
                    Text(gs(R.string.rs_roles_shadows_empty), style = UType.Sub, modifier = Modifier.padding(vertical = 10.dp))
                } else {
                    w.shadows.forEachIndexed { i, sh ->
                        if (i > 0) HorizontalDivider(color = U.c.line)
                        RoleRow(sh, w, onClick = { go.role(sh.id) })
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SoftButton(stringResource(R.string.rs_roles_name_shadow), { go.library(shadows = true) }, Modifier.weight(1f), color = U.c.shadow)
                SoftButton(stringResource(R.string.rs_roles_own_shadow), { go.roleForm(shadow = true) }, Modifier.weight(1f), color = U.c.shadow, icon = Icons.Rounded.Edit)
            }
        }
    }
}

/** The core: the white light every role is made of. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CoreCard(data: WData, go: Go) {
    val c = U.c
    Box(
        Modifier
            .fillMaxWidth()
            .clip(U.RLg)
            .background(c.surface.copy(alpha = if (c.dark) 0.85f else 1f))
            .border(1.dp, c.accent.copy(alpha = 0.35f), U.RLg)
            .clickable { go.core() },
    ) {
        Canvas(Modifier.matchParentSize()) {
            drawRect(
                Brush.linearGradient(
                    listOf(c.accent.copy(alpha = 0.16f), c.accent.copy(alpha = 0f)),
                    start = Offset(0f, 0f), end = Offset(size.width * 0.8f, size.height),
                ),
            )
        }
        Column(Modifier.padding(18.dp)) {
            Text(stringResource(R.string.rs_roles_core).uppercase(), style = UType.Eyebrow.copy(color = c.accent))
            Spacer(Modifier.height(6.dp))
            Text(gs(R.string.rs_roles_core_sub), style = UType.Muted)
            Spacer(Modifier.height(12.dp))
            val values = data.world.core
            if (values.isEmpty()) {
                Text(gs(R.string.rs_roles_core_empty), style = UType.Body.copy(color = c.accent))
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    values.forEach { v ->
                        Text(
                            v.text,
                            style = UType.Strong.copy(color = c.accent),
                            modifier = Modifier.clip(U.RSm).background(c.accent.copy(alpha = 0.10f)).padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SphereCard(s: Sphere, data: WData, go: Go) {
    val w = data.world
    val hue = U.c.hue(s.hue)
    val roles = w.rolesIn(s.id)
    UCard(onClick = { go.sphere(s.id) }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconDisc(sphereIcon(s), 42.dp, hue)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(sphereName(s), style = UType.Heading, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    if (roles.isEmpty()) gs(R.string.rs_roles_sphere_empty) else gp(R.plurals.roles, roles.size, roles.size),
                    style = UType.Muted,
                )
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = U.c.faint)
        }
        if (roles.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                roles.forEach { r ->
                    Column(Modifier.widthIn(max = 84.dp).clickable { go.role(r.id) }, horizontalAlignment = Alignment.CenterHorizontally) {
                        Facet(roleColor(w, r), Modifier.size(34.dp), cut = w.stats(r.id).cut.ordinal)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            roleName(r), style = UType.Small.copy(color = U.c.muted, fontSize = 11.5.sp, textAlign = TextAlign.Center),
                            maxLines = 2, overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}
