package app.prism.ui.today

import androidx.compose.foundation.Canvas
import app.prism.ui.quoted
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.prism.R
import app.prism.data.Role
import app.prism.domain.World
import app.prism.ui.components.Chip
import app.prism.ui.components.Facet
import app.prism.ui.components.UCard
import app.prism.ui.components.UTextField
import app.prism.ui.components.roleColor
import app.prism.ui.gs
import app.prism.ui.roleName
import app.prism.ui.sphereName
import app.prism.ui.theme.U
import app.prism.ui.theme.UType

/** One scene being rated: how close to the role, how it felt, who interfered. */
data class Draft(val score: Int = 70, val energy: Int = 0, val shadowId: Long = 0, val note: String = "")

/** A little gauge for energy -2..2: an empty ring for drained, a full disc for charged. */
@Composable
fun EnergyDot(level: Int, modifier: Modifier = Modifier, size: Dp = 18.dp, color: Color = U.c.accent) {
    Canvas(modifier.size(size)) {
        val r = this.size.minDimension / 2 - 1.5f * density
        val c = Offset(this.size.width / 2, this.size.height / 2)
        drawCircle(color, r, c, style = Stroke(1.5f * density))
        val f = (level + 2) / 4f
        if (f > 0f) {
            // Fill from the bottom like a glass of light.
            val top = c.y + r - 2 * r * f
            drawContext.canvas.save()
            drawContext.canvas.clipRect(0f, top, this.size.width, this.size.height)
            drawCircle(color.copy(alpha = 0.85f), r - 1f * density, c)
            drawContext.canvas.restore()
        }
    }
}

@Composable
fun energyLabel(level: Int): String = stringResource(
    when (level) {
        -2 -> R.string.scene_energy_m2
        -1 -> R.string.scene_energy_m1
        1 -> R.string.scene_energy_p1
        2 -> R.string.scene_energy_p2
        else -> R.string.scene_energy_0
    }
)

/** Score 0..100 with three anchors underneath. */
@Composable
fun ScoreSlider(value: Int, onChange: (Int) -> Unit, color: Color) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.scene_score_title), style = UType.Muted, modifier = Modifier.weight(1f))
            Text("$value%", style = UType.Strong.copy(color = color))
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(Math.round(it)) },
            valueRange = 0f..100f,
            colors = SliderDefaults.colors(thumbColor = color, activeTrackColor = color, inactiveTrackColor = U.c.line),
        )
        Row {
            Text(stringResource(R.string.scene_score_far), style = UType.Small, modifier = Modifier.weight(1f))
            Text(stringResource(R.string.scene_score_half), style = UType.Small, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            Text(stringResource(R.string.scene_score_spot), style = UType.Small, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
        }
    }
}

/** Five glyphs from drained to charged. */
@Composable
fun EnergyPicker(value: Int, onChange: (Int) -> Unit, color: Color) {
    Column {
        Text(stringResource(R.string.scene_energy_title), style = UType.Muted)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            (-2..2).forEach { lv ->
                val on = lv == value
                Column(
                    Modifier
                        .weight(1f)
                        .clip(U.RSm)
                        .background(if (on) color.copy(alpha = 0.16f) else U.c.surface2)
                        .border(1.dp, if (on) color.copy(alpha = 0.6f) else U.c.line, U.RSm)
                        .clickable { onChange(lv) }
                        .padding(vertical = 8.dp, horizontal = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    EnergyDot(lv, color = if (on) color else U.c.muted)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        energyLabel(lv),
                        style = UType.Small.copy(fontSize = 10.5.sp, lineHeight = 12.sp, color = if (on) color else U.c.faint, textAlign = androidx.compose.ui.text.style.TextAlign.Center),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/** "Did a shadow show up?": tap one, tap again to clear. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ShadowChips(w: World, selected: Long, onSelect: (Long) -> Unit) {
    if (w.shadows.isEmpty()) return
    Column {
        Text(gs(R.string.scene_shadow_q), style = UType.Muted)
        Spacer(Modifier.height(6.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            w.shadows.forEach { s ->
                Chip(roleName(s), selected = s.id == selected, onClick = { onSelect(if (s.id == selected) 0 else s.id) }, color = U.c.shadow)
            }
        }
    }
}

/** A role's whole rating card: header, score, energy, shadow, note. */
@Composable
fun SceneEditor(role: Role, w: World, draft: Draft, onChange: (Draft) -> Unit, modifier: Modifier = Modifier) {
    val color = roleColor(w, role)
    UCard(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Facet(color, Modifier.size(34.dp), cut = w.stats(role.id).cut.ordinal)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(roleName(role), style = UType.Strong)
                Text(sphereName(w.sphere(role.sphereId)), style = UType.Small)
            }
        }
        if (role.motto.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(quoted(role.motto), style = UType.Motto.copy(fontSize = 15.sp, color = U.c.muted))
        }
        Spacer(Modifier.height(14.dp))
        ScoreSlider(draft.score, { onChange(draft.copy(score = it)) }, color)
        Spacer(Modifier.height(16.dp))
        EnergyPicker(draft.energy, { onChange(draft.copy(energy = it)) }, color)
        if (w.shadows.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            ShadowChips(w, draft.shadowId) { onChange(draft.copy(shadowId = it)) }
        }
        Spacer(Modifier.height(12.dp))
        UTextField(draft.note, { onChange(draft.copy(note = it)) }, placeholder = stringResource(R.string.scene_note_hint), minLines = 2)
    }
}

/** A thin coloured dot. */
@Composable
fun Dot(color: Color, size: Dp = 10.dp) {
    Box(Modifier.size(size).clip(androidx.compose.foundation.shape.CircleShape).background(color))
}
