package app.prism.ui.practices

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.dp
import app.prism.ui.components.Chip
import app.prism.ui.components.TopBar
import app.prism.ui.theme.UType
import org.json.JSONArray
import org.json.JSONObject

/** A practice screen: a top bar over a padded list. */
@Composable
fun PracticeScaffold(
    title: String,
    onBack: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
    content: LazyListScope.() -> Unit,
) {
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        TopBar(title, onBack, actions)
        LazyColumn(
            Modifier.weight(1f).imePadding(),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 4.dp, bottom = 32.dp),
        ) {
            content()
            item { Spacer(Modifier.navigationBarsPadding()) }
        }
    }
}

/** A label above a field. */
@Composable
fun FieldLabel(text: String) {
    Text(text, style = UType.Muted, modifier = Modifier.padding(top = 14.dp, bottom = 6.dp, start = 2.dp))
}

/** Tap-to-fill suggestions. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Suggestions(options: List<String>, selected: (String) -> Boolean = { false }, onPick: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { o -> Chip(o, selected(o), { onPick(o) }) }
    }
}

@Composable
fun ColumnScope.Gap(h: Int = 12) = Spacer(Modifier.height(h.dp))

fun HapticFeedback.tick(on: Boolean) {
    if (on) performHapticFeedback(HapticFeedbackType.TextHandleMove)
}

fun HapticFeedback.thump(on: Boolean) {
    if (on) performHapticFeedback(HapticFeedbackType.LongPress)
}

/** Shares plain text through the system sheet. */
fun shareText(context: Context, title: String, text: String) {
    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_SUBJECT, title).putExtra(Intent.EXTRA_TEXT, text)
    context.startActivity(Intent.createChooser(send, title))
}

fun json(body: String): JSONObject = runCatching { JSONObject(body) }.getOrDefault(JSONObject())

fun JSONObject.str(key: String): String = optString(key, "")

fun JSONObject.strings(key: String): List<String> {
    val a = optJSONArray(key) ?: return emptyList()
    return List(a.length()) { a.optString(it, "") }
}

fun stringsArray(list: List<String>): JSONArray = JSONArray().apply { list.forEach { put(it) } }

