package app.prism.ui.components

import androidx.compose.foundation.clickable
import app.prism.ui.quoted
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Anchor
import androidx.compose.material.icons.rounded.Balance
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Diversity3
import androidx.compose.material.icons.rounded.DoorFront
import androidx.compose.material.icons.rounded.EmojiPeople
import androidx.compose.material.icons.rounded.Flare
import androidx.compose.material.icons.rounded.Landscape
import androidx.compose.material.icons.rounded.Mail
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.TheaterComedy
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.prism.R
import app.prism.data.Role
import app.prism.domain.Cut
import app.prism.domain.Practice
import app.prism.domain.World
import app.prism.ui.roleName
import app.prism.ui.sphereName
import app.prism.ui.theme.U
import app.prism.ui.theme.UType

/** A role's colour: its own or its sphere's; shadows are smoky violet. */
@Composable
fun roleColor(w: World, r: Role?): Color = when {
    r == null -> U.c.faint
    r.shadow -> U.c.shadow
    else -> U.c.hue(w.hueOf(r))
}

/** A role in a list: its facet, name, sphere and motto. */
@Composable
fun RoleRow(
    role: Role,
    w: World,
    modifier: Modifier = Modifier,
    showSphere: Boolean = true,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    val color = roleColor(w, role)
    Row(
        modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Facet(color, Modifier.size(38.dp), cut = w.stats(role.id).cut.ordinal, shadow = role.shadow)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(roleName(role), style = UType.Strong, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val sub = when {
                role.motto.isNotBlank() -> quoted(role.motto)
                role.tagline.isNotBlank() -> role.tagline
                showSphere -> sphereName(w.sphere(role.sphereId))
                else -> ""
            }
            if (sub.isNotBlank()) Text(sub, style = UType.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (trailing != null) trailing()
        else if (onClick != null) Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = U.c.faint)
    }
}

/** Pick one of the user's roles (or shadows). Shows a hint when there are none. */
@Composable
fun RolePickerDialog(
    w: World,
    title: String,
    onPick: (Role) -> Unit,
    onDismiss: () -> Unit,
    shadows: Boolean = false,
) {
    val list = if (shadows) w.shadows else w.roles
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = U.c.surface2,
        title = { Text(title, style = UType.Heading) },
        text = {
            if (list.isEmpty()) Text(stringResource(if (shadows) R.string.picker_no_shadows else R.string.picker_no_roles), style = UType.Sub)
            else LazyColumn(Modifier.heightIn(max = 420.dp)) {
                items(list, key = { it.id }) { r ->
                    RoleRow(r, w, onClick = { onPick(r); onDismiss() }, trailing = {})
                    HorizontalDivider(color = U.c.line)
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = U.c.muted, style = UType.Button) } },
    )
}

@Composable
fun cutName(c: Cut): String = stringResource(
    when (c) {
        Cut.SKETCH -> R.string.cut_sketch
        Cut.FACET -> R.string.cut_facet
        Cut.POLISHED -> R.string.cut_polished
        Cut.BRILLIANT -> R.string.cut_brilliant
    }
)

@Composable
fun practiceName(p: Practice): String = stringResource(
    when (p) {
        Practice.MORNING -> R.string.pr_morning
        Practice.THRESHOLD -> R.string.pr_threshold
        Practice.REHEARSAL -> R.string.pr_rehearsal
        Practice.SOS -> R.string.pr_sos
        Practice.REVIEW -> R.string.pr_review
        Practice.COUNCIL -> R.string.pr_council
        Practice.LETTER -> R.string.pr_letter
        Practice.OTHERS -> R.string.pr_others
        Practice.SPEECH -> R.string.pr_speech
        Practice.SHADOW_TALK -> R.string.pr_shadow_talk
        Practice.MODEL -> R.string.pr_model
        Practice.ANCHOR -> R.string.pr_anchor
        Practice.ACT -> R.string.pr_act
    }
)

@Composable
fun practiceBlurb(p: Practice): String = stringResource(
    when (p) {
        Practice.MORNING -> R.string.pr_morning_blurb
        Practice.THRESHOLD -> R.string.pr_threshold_blurb
        Practice.REHEARSAL -> R.string.pr_rehearsal_blurb
        Practice.SOS -> R.string.pr_sos_blurb
        Practice.REVIEW -> R.string.pr_review_blurb
        Practice.COUNCIL -> R.string.pr_council_blurb
        Practice.LETTER -> R.string.pr_letter_blurb
        Practice.OTHERS -> R.string.pr_others_blurb
        Practice.SPEECH -> R.string.pr_speech_blurb
        Practice.SHADOW_TALK -> R.string.pr_shadow_talk_blurb
        Practice.MODEL -> R.string.pr_model_blurb
        Practice.ANCHOR -> R.string.pr_anchor_blurb
        Practice.ACT -> R.string.pr_act_blurb
    }
)

fun practiceIcon(p: Practice): ImageVector = when (p) {
    Practice.MORNING -> Icons.Rounded.WbSunny
    Practice.THRESHOLD -> Icons.Rounded.DoorFront
    Practice.REHEARSAL -> Icons.Rounded.TheaterComedy
    Practice.SOS -> Icons.Rounded.Landscape
    Practice.REVIEW -> Icons.Rounded.Bedtime
    Practice.COUNCIL -> Icons.Rounded.Diversity3
    Practice.LETTER -> Icons.Rounded.Mail
    Practice.OTHERS -> Icons.Rounded.Visibility
    Practice.SPEECH -> Icons.Rounded.RecordVoiceOver
    Practice.SHADOW_TALK -> Icons.Rounded.Flare
    Practice.MODEL -> Icons.Rounded.EmojiPeople
    Practice.ANCHOR -> Icons.Rounded.Anchor
    Practice.ACT -> Icons.Rounded.Balance
}
