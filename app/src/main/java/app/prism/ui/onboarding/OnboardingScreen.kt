package app.prism.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.prism.R
import app.prism.container
import app.prism.content.DefaultSpheres
import app.prism.content.SpherePresets
import app.prism.data.setCore
import app.prism.data.setupSpheres
import app.prism.domain.Gender
import app.prism.ui.LocalPerson
import app.prism.ui.Person
import app.prism.ui.SphereIcon
import app.prism.ui.WData
import app.prism.ui.components.Beam
import app.prism.ui.components.Chip
import app.prism.ui.components.PrimaryButton
import app.prism.ui.components.PrismView
import app.prism.ui.components.RoundIcon
import app.prism.ui.components.SoftButton
import app.prism.ui.components.UTextField
import app.prism.ui.g
import app.prism.ui.gs
import app.prism.ui.library
import app.prism.ui.locale
import app.prism.ui.theme.U
import app.prism.ui.theme.UType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val PAGES = 5

@Composable
fun OnboardingScreen(data: WData, onDone: (openLibrary: Boolean) -> Unit) {
    val context = LocalContext.current
    val container = context.container
    val russian = locale().language == "ru"
    var page by rememberSaveable { mutableIntStateOf(0) }
    var name by rememberSaveable { mutableStateOf(data.settings.name) }
    var gender by rememberSaveable { mutableStateOf(data.settings.gender) }
    val values = remember { data.world.core.map { it.text }.toMutableStateList() }
    val spheres = remember { DefaultSpheres.toMutableStateList() }
    var saving by remember { mutableStateOf(false) }

    BackHandler(enabled = page > 0) { page-- }

    fun finish(openLibrary: Boolean) {
        if (saving) return
        saving = true
        container.scope.launch {
            val dao = container.dao
            dao.setCore(values.toList())
            // Keep the order of the preset list, so spheres don't end up shuffled by tap order.
            dao.setupSpheres(SpherePresets.map { it.key }.filter { it in spheres })
            container.settings.update { it.copy(onboarded = true, name = name.trim(), gender = gender) }
            container.changed()
            withContext(Dispatchers.Main) { onDone(openLibrary) }
        }
    }

    CompositionLocalProvider(LocalPerson provides Person(name, gender)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
            Dots(page, Modifier.padding(top = 18.dp).align(Alignment.CenterHorizontally))
            AnimatedContent(
                targetState = page,
                transitionSpec = { fadeIn(tween(260)) togetherWith fadeOut(tween(180)) },
                modifier = Modifier.weight(1f),
                label = "page",
            ) { p ->
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 18.dp)) {
                    when (p) {
                        0 -> Welcome()
                        1 -> NamePage(name, { name = it.take(30) }, russian, gender) { gender = it }
                        2 -> CorePage(values)
                        3 -> SpheresPage(spheres)
                        else -> FinishPage()
                    }
                }
            }
            Column(Modifier.padding(horizontal = 22.dp, vertical = 14.dp)) {
                when (page) {
                    0 -> PrimaryButton(stringResource(R.string.ob_start), { page = 1 }, Modifier.fillMaxWidth())
                    PAGES - 1 -> {
                        PrimaryButton(stringResource(R.string.ob_finish_library), { finish(true) }, Modifier.fillMaxWidth(), enabled = !saving)
                        Spacer(Modifier.height(10.dp))
                        SoftButton(stringResource(R.string.ob_finish_look), { finish(false) }, Modifier.fillMaxWidth())
                    }
                    else -> {
                        val ok = when (page) {
                            2 -> values.size in 1..5
                            3 -> spheres.isNotEmpty()
                            else -> true
                        }
                        PrimaryButton(stringResource(R.string.next), { page++ }, Modifier.fillMaxWidth(), enabled = ok)
                    }
                }
            }
        }
    }
}

@Composable
private fun Dots(page: Int, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(PAGES) { i ->
            Box(
                Modifier
                    .size(width = if (i == page) 18.dp else 6.dp, height = 6.dp)
                    .clip(CircleShape)
                    .background(if (i == page) U.c.accent else U.c.line),
            )
        }
    }
}

@Composable
private fun Welcome() {
    val hues = U.c.hues
    val beams = remember(hues) {
        listOf(0 to 0.9f, 2 to 0.6f, 4 to 1f, 6 to 0.7f, 8 to 0.85f, 10 to 0.5f).map { (h, b) -> Beam(hues[h], 1f + b, b) }
    }
    Spacer(Modifier.height(24.dp))
    PrismView(beams, Modifier.fillMaxWidth().height(230.dp), labels = false)
    Spacer(Modifier.height(30.dp))
    Text(stringResource(R.string.ob_welcome_title), style = UType.Display)
    Spacer(Modifier.height(14.dp))
    Text(gs(R.string.ob_welcome_text), style = UType.Body)
    Spacer(Modifier.height(10.dp))
    Text(gs(R.string.ob_welcome_more), style = UType.Sub)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NamePage(name: String, onName: (String) -> Unit, russian: Boolean, gender: Gender, onGender: (Gender) -> Unit) {
    Spacer(Modifier.height(30.dp))
    Text(stringResource(R.string.ob_name_title), style = UType.Title)
    Spacer(Modifier.height(8.dp))
    Text(stringResource(R.string.ob_name_text), style = UType.Sub)
    Spacer(Modifier.height(20.dp))
    UTextField(name, onName, placeholder = stringResource(R.string.ob_name_hint))
    if (russian) {
        Spacer(Modifier.height(28.dp))
        Text(stringResource(R.string.ob_gender_title), style = UType.Heading)
        Spacer(Modifier.height(10.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip(stringResource(R.string.gender_female), gender == Gender.FEMALE, { onGender(Gender.FEMALE) })
            Chip(stringResource(R.string.gender_male), gender == Gender.MALE, { onGender(Gender.MALE) })
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CorePage(values: MutableList<String>) {
    val ideas = stringArrayResource(R.array.core_suggestions)
    var custom by remember { mutableStateOf("") }
    fun toggle(v: String) {
        if (v in values) values.remove(v) else if (values.size < 5) values.add(v)
    }
    Spacer(Modifier.height(30.dp))
    Text(stringResource(R.string.ob_core_title), style = UType.Title)
    Spacer(Modifier.height(8.dp))
    Text(stringResource(R.string.ob_core_text), style = UType.Sub)
    Spacer(Modifier.height(6.dp))
    Text(stringResource(R.string.ob_core_count, values.size), style = UType.Small.copy(color = if (values.size >= 3) U.c.accent else U.c.faint))
    Spacer(Modifier.height(16.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        // The user's own values first, then the ideas.
        (values.filter { it !in ideas } + ideas).forEach { v -> Chip(v, v in values, { toggle(v) }) }
    }
    Spacer(Modifier.height(16.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        UTextField(custom, { custom = it.take(30) }, Modifier.weight(1f), placeholder = stringResource(R.string.ob_core_custom))
        Spacer(Modifier.width(10.dp))
        RoundIcon(Icons.Rounded.Add, enabled = custom.isNotBlank() && values.size < 5, size = 44.dp) {
            val v = custom.trim().replaceFirstChar { it.uppercaseChar() }
            if (v !in values) values.add(v)
            custom = ""
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SpheresPage(spheres: MutableList<String>) {
    val lib = library()
    Spacer(Modifier.height(30.dp))
    Text(stringResource(R.string.ob_spheres_title), style = UType.Title)
    Spacer(Modifier.height(8.dp))
    Text(gs(R.string.ob_spheres_text), style = UType.Sub)
    Spacer(Modifier.height(18.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        SpherePresets.forEach { p ->
            val on = p.key in spheres
            Chip(
                g(lib.spheres[p.key]?.name.orEmpty()),
                on,
                { if (on) spheres.remove(p.key) else spheres.add(p.key) },
                color = U.c.hue(p.hue),
                icon = SphereIcon.of(p.icon).icon,
            )
        }
    }
    Spacer(Modifier.height(16.dp))
    Text(stringResource(R.string.ob_spheres_note), style = UType.Muted)
}

@Composable
private fun FinishPage() {
    Spacer(Modifier.height(40.dp))
    app.prism.ui.components.PrismGlyph(Modifier.fillMaxWidth().height(120.dp))
    Spacer(Modifier.height(30.dp))
    Text(stringResource(R.string.ob_finish_title), style = UType.Display, textAlign = TextAlign.Start)
    Spacer(Modifier.height(12.dp))
    Text(stringResource(R.string.ob_finish_text), style = UType.Body)
}
