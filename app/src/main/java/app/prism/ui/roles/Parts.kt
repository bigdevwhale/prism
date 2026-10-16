package app.prism.ui.roles

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import app.prism.domain.lines
import app.prism.ui.components.SectionTitle
import app.prism.ui.components.UCard
import app.prism.ui.theme.U
import app.prism.ui.theme.UType

/** A titled block of plain text; nothing when the text is blank. */
@Composable
internal fun TextSection(title: String, text: String) {
    if (text.isBlank()) return
    SectionTitle(title)
    UCard { Text(text, style = UType.Body) }
}

/** A titled list with a mark in front of each line: a check for "does", a cross for "never". */
@Composable
internal fun ListSection(title: String, items: List<String>, icon: ImageVector, tint: Color) {
    if (items.isEmpty()) return
    SectionTitle(title)
    UCard {
        items.forEachIndexed { i, s ->
            if (i > 0) Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.Top) {
                Icon(icon, null, Modifier.padding(top = 2.dp).size(18.dp), tint = tint)
                Spacer(Modifier.width(10.dp))
                Text(s, style = UType.Body, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
internal fun DoesSection(title: String, text: String, color: Color) = ListSection(title, lines(text), Icons.Rounded.Check, color)

@Composable
internal fun NeverSection(title: String, text: String) = ListSection(title, lines(text), Icons.Rounded.Close, U.c.danger)

/** Small chips for qualities. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun QualityChips(items: List<String>, color: Color) {
    if (items.isEmpty()) return
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { q ->
            Box(
                Modifier
                    .clip(U.RSm)
                    .background(color.copy(alpha = 0.14f))
                    .border(1.dp, color.copy(alpha = 0.35f), U.RSm)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) { Text(q, style = UType.Strong.copy(color = color, fontSize = UType.Body.fontSize)) }
        }
    }
}

/** A stat in a row of three: a number and its label. */
@Composable
internal fun Stat(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = UType.Number)
        Spacer(Modifier.height(2.dp))
        Text(label, style = UType.Small)
    }
}

/** The 12 spectrum swatches; [selected] -1 means "none" (shown only when [none] is given). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun HuePicker(selected: Int, onSelect: (Int) -> Unit, none: Color? = null) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (none != null) Swatch(none, selected == -1, dashed = true) { onSelect(-1) }
        U.c.hues.forEachIndexed { i, c -> Swatch(c, selected == i) { onSelect(i) } }
    }
}

@Composable
private fun Swatch(color: Color, on: Boolean, dashed: Boolean = false, onClick: () -> Unit) {
    Box(
        Modifier
            .size(38.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .border(if (on) 2.5.dp else 1.dp, if (on) U.c.ink else U.c.line, CircleShape)
            .padding(5.dp)
            .clip(CircleShape)
            .background(if (dashed) color.copy(alpha = 0.35f) else color),
    )
}

internal val SidePad = 18.dp

internal fun Modifier.side() = this.fillMaxWidth().padding(horizontal = SidePad)
