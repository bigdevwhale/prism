package app.prism.ui.today

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.prism.R
import app.prism.container
import app.prism.data.Scene
import app.prism.ui.Go
import app.prism.ui.WData
import app.prism.ui.components.EmptyState
import app.prism.ui.components.PrimaryButton
import app.prism.ui.components.RolePickerDialog
import app.prism.ui.components.SoftButton
import app.prism.ui.components.TopBar
import app.prism.ui.gs
import app.prism.ui.theme.U
import app.prism.ui.theme.UType

/** One scene, rated right after it happened. */
@Composable
fun QuickSceneScreen(roleId: Long, data: WData, go: Go) {
    val w = data.world
    val context = LocalContext.current
    var id by rememberSaveable { mutableLongStateOf(if (roleId != 0L) roleId else w.focus?.id ?: 0L) }
    var draft by remember { mutableStateOf(Draft()) }
    var picking by remember { mutableStateOf(false) }
    val role = w.role(id)

    Column(Modifier.fillMaxSize().statusBarsPadding().imePadding()) {
        TopBar(stringResource(R.string.home_quick), onBack = { go.back() })
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 20.dp)) {
            item {
                Text(gs(R.string.quick_lead), style = UType.Sub, modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 12.dp))
            }
            if (role == null) {
                item {
                    EmptyState(
                        stringResource(R.string.quick_pick),
                        if (w.roles.isEmpty()) null else stringResource(R.string.pick_role),
                        { picking = true },
                    )
                }
            } else {
                item {
                    SceneEditor(role, w, draft, { draft = it })
                    Spacer(Modifier.height(10.dp))
                    SoftButton(stringResource(R.string.quick_other_role), { picking = true }, Modifier.fillMaxWidth(), color = U.c.muted, icon = Icons.Rounded.SwapHoriz)
                }
            }
        }
        PrimaryButton(
            stringResource(R.string.save),
            onClick = {
                val r = role ?: return@PrimaryButton
                val d = draft
                val now = System.currentTimeMillis()
                val day = data.day
                context.container.write {
                    insertScene(Scene(day = day, at = now, roleId = r.id, kind = Scene.QUICK, score = d.score, energy = d.energy, shadowId = d.shadowId, note = d.note.trim()))
                }
                go.back()
            },
            enabled = role != null,
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 18.dp, vertical = 12.dp),
        )
    }
    if (picking) RolePickerDialog(w, stringResource(R.string.pick_role), onPick = { id = it.id }, onDismiss = { picking = false })
}
