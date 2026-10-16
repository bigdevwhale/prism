package app.prism.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.prism.R
import app.prism.container
import app.prism.data.setCore
import app.prism.ui.Go
import app.prism.ui.WData
import app.prism.ui.components.Chip
import app.prism.ui.components.PrimaryButton
import app.prism.ui.components.PrismGlyph
import app.prism.ui.components.RoundIcon
import app.prism.ui.components.SectionTitle
import app.prism.ui.components.TopBar
import app.prism.ui.components.UCard
import app.prism.ui.components.UTextField
import app.prism.ui.gs
import app.prism.ui.theme.U
import app.prism.ui.theme.UType

private const val MAX = 7

/** The core: the values that stay the same in every role. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CoreScreen(data: WData, go: Go) {
    val container = LocalContext.current.container
    val values = remember(data.world.core) { data.world.core.map { it.text }.toMutableStateList() }
    var custom by remember { mutableStateOf("") }
    val ideas = stringArrayResource(R.array.core_suggestions).filter { it !in values }

    fun add(v: String) {
        val t = v.trim().replaceFirstChar { it.uppercaseChar() }
        if (t.isNotEmpty() && t !in values && values.size < MAX) values.add(t)
    }

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
        TopBar(stringResource(R.string.core_title), { go.back() })
        LazyColumn(Modifier.weight(1f).padding(horizontal = 18.dp)) {
            item {
                PrismGlyph(Modifier.fillMaxWidth().height(90.dp))
                Spacer(Modifier.height(14.dp))
                Text(gs(R.string.core_lead), style = UType.Body)
                Spacer(Modifier.height(6.dp))
                Text(stringResource(R.string.core_hint), style = UType.Muted)
                SectionTitle(stringResource(R.string.core_mine))
            }
            if (values.isEmpty()) item { Text(stringResource(R.string.core_empty), style = UType.Sub) }
            else item {
                UCard(padding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 4.dp)) {
                    values.forEachIndexed { i, v ->
                        if (i > 0) HorizontalDivider(color = U.c.line)
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(v, style = UType.Strong, modifier = Modifier.weight(1f))
                            IconButton(onClick = { values.add(i - 1, values.removeAt(i)) }, enabled = i > 0) {
                                Icon(Icons.Rounded.KeyboardArrowUp, stringResource(R.string.core_up), tint = if (i > 0) U.c.muted else U.c.line)
                            }
                            IconButton(onClick = { values.add(i + 1, values.removeAt(i)) }, enabled = i < values.size - 1) {
                                Icon(Icons.Rounded.KeyboardArrowDown, stringResource(R.string.core_down), tint = if (i < values.size - 1) U.c.muted else U.c.line)
                            }
                            IconButton(onClick = { values.removeAt(i) }) {
                                Icon(Icons.Rounded.Close, stringResource(R.string.remove), Modifier.size(20.dp), tint = U.c.faint)
                            }
                        }
                    }
                }
            }
            item {
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    UTextField(custom, { custom = it.take(30) }, Modifier.weight(1f), placeholder = stringResource(R.string.core_add_hint))
                    Spacer(Modifier.width(10.dp))
                    RoundIcon(Icons.Rounded.Add, enabled = custom.isNotBlank() && values.size < MAX, size = 44.dp) { add(custom); custom = "" }
                }
            }
            if (ideas.isNotEmpty() && values.size < MAX) item {
                SectionTitle(stringResource(R.string.core_ideas))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    ideas.forEach { Chip(it, false, { add(it) }) }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
        PrimaryButton(
            stringResource(R.string.save),
            {
                val list = values.toList()
                container.write { setCore(list) }
                go.back()
            },
            Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
            enabled = values.isNotEmpty(),
        )
    }
}
