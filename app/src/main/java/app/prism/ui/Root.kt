package app.prism.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Diamond
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.prism.BuildConfig
import app.prism.R
import app.prism.container
import app.prism.data.Settings
import app.prism.domain.Practice
import app.prism.domain.World
import app.prism.notify.Alarms
import app.prism.ui.components.Backdrop
import app.prism.ui.components.rememberNow
import app.prism.ui.journal.JournalTab
import app.prism.ui.onboarding.CoreScreen
import app.prism.ui.onboarding.OnboardingScreen
import app.prism.ui.practices.ActScreen
import app.prism.ui.practices.AnchorScreen
import app.prism.ui.practices.CouncilScreen
import app.prism.ui.practices.NoteScreen
import app.prism.ui.practices.PracticesTab
import app.prism.ui.practices.RehearsalScreen
import app.prism.ui.practices.SosScreen
import app.prism.ui.practices.ThresholdScreen
import app.prism.ui.practices.ThresholdsScreen
import app.prism.ui.roles.LibraryScreen
import app.prism.ui.roles.PresetScreen
import app.prism.ui.roles.RoleFormScreen
import app.prism.ui.roles.RoleScreen
import app.prism.ui.roles.RolesTab
import app.prism.ui.roles.SphereFormScreen
import app.prism.ui.roles.SphereScreen
import app.prism.ui.settings.SettingsScreen
import app.prism.ui.theme.Inter
import app.prism.ui.theme.U
import app.prism.ui.today.MorningScreen
import app.prism.ui.today.PrismTab
import app.prism.ui.today.QuickSceneScreen
import app.prism.ui.today.ReviewScreen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

enum class Tab { PRISM, ROLES, PRACTICES, JOURNAL }

/** Everything stored plus the settings, rebuilt once a minute or on any change. */
@Immutable
class WData(val world: World, val settings: Settings, val now: Long) {
    val today: LocalDate get() = world.today
    val day: Long get() = world.day
}

/** Everything a screen can open. Ids of 0 mean "new". */
class Go(private val nav: NavHostController) {
    fun sphere(id: Long) = nav.navigate("sphere/$id")
    fun sphereForm(id: Long = 0) = nav.navigate("sphereForm/$id")
    fun role(id: Long) = nav.navigate("role/$id")
    /** The role editor: an existing role, or a new role (or shadow) in a sphere. */
    fun roleForm(id: Long = 0, sphereId: Long = 0, shadow: Boolean = false) = nav.navigate("roleForm?id=$id&sphere=$sphereId&shadow=$shadow")
    /** The library, optionally opened on one sphere ("" = all) or on the shadows. */
    fun library(sphereKey: String = "", shadows: Boolean = false) = nav.navigate("library?sphere=$sphereKey&shadows=$shadows")
    /** A library role or shadow, before adopting it. */
    fun preset(key: String) = nav.navigate("preset/$key")
    fun core() = nav.navigate("core")
    fun morning() = nav.navigate("morning")
    fun review() = nav.navigate("review")
    fun quick(roleId: Long = 0) = nav.navigate("quick?role=$roleId")
    fun threshold(roleId: Long) = nav.navigate("threshold/$roleId")
    fun thresholds() = nav.navigate("thresholds")
    fun sos() = nav.navigate("sos")
    fun rehearsal(id: Long = 0, roleId: Long = 0) = nav.navigate("rehearsal/$id?role=$roleId")
    fun council(id: Long = 0) = nav.navigate("council/$id")
    /** A written practice ([Practice.written]): a new one, or an existing note. */
    fun note(kind: Practice, id: Long = 0, roleId: Long = 0) = nav.navigate("note/${kind.name}/$id?role=$roleId")
    fun anchor(roleId: Long) = nav.navigate("anchor/$roleId")
    /** An act: an existing one, or a new one for [roleId]. */
    fun act(id: Long = 0, roleId: Long = 0) = nav.navigate("act/$id?role=$roleId")
    fun settings() = nav.navigate("settings")
    fun back() = nav.popBackStack()
    fun home() = nav.popBackStack("main", inclusive = false)
    /** After creating something: replace the form with the thing itself. */
    fun replaceWithRole(id: Long) = nav.navigate("role/$id") { popUpTo("main") }
}

@Composable
fun PrismRoot(openRoute: MutableStateFlow<String?>, settings: Settings) {
    val context = LocalContext.current
    val container = context.container
    val nav = rememberNavController()
    val go = remember(nav) { Go(nav) }
    var tab by rememberSaveable { mutableStateOf(Tab.PRISM) }
    val start = remember { if (settings.onboarded) "main" else "welcome" }

    val dao = container.dao
    val core by dao.observeCore().collectAsStateWithLifecycle(initialValue = emptyList())
    val spheres by dao.observeSpheres().collectAsStateWithLifecycle(initialValue = emptyList())
    val roles by dao.observeRoles().collectAsStateWithLifecycle(initialValue = null)
    val scenes by dao.observeScenes().collectAsStateWithLifecycle(initialValue = emptyList())
    val days by dao.observeDays().collectAsStateWithLifecycle(initialValue = emptyList())
    val rehearsals by dao.observeRehearsals().collectAsStateWithLifecycle(initialValue = emptyList())
    val thresholds by dao.observeThresholds().collectAsStateWithLifecycle(initialValue = emptyList())
    val acts by dao.observeActs().collectAsStateWithLifecycle(initialValue = emptyList())
    val actDays by dao.observeActDays().collectAsStateWithLifecycle(initialValue = emptyList())
    val notes by dao.observeNotes().collectAsStateWithLifecycle(initialValue = emptyList())
    val minute = rememberNow(60_000)
    val day = remember(minute) { LocalDate.now(ZoneId.systemDefault()) }
    val data = remember(core, spheres, roles, scenes, days, rehearsals, thresholds, acts, actDays, notes, settings, day) {
        WData(
            World(core, spheres, roles.orEmpty(), scenes, days, rehearsals, thresholds, acts, actDays, notes, day),
            settings,
            System.currentTimeMillis(),
        )
    }

    val requested by openRoute.collectAsStateWithLifecycle()
    LaunchedEffect(requested, settings.onboarded, roles != null) {
        val route = requested ?: return@LaunchedEffect
        if (!settings.onboarded || roles == null) return@LaunchedEffect
        nav.popBackStack("main", inclusive = false)
        tab = Tab.PRISM
        when {
            route == "morning" -> go.morning()
            route == "review" -> go.review()
            route.startsWith("threshold/") -> route.removePrefix("threshold/").toLongOrNull()?.let { go.threshold(it) }
        }
        openRoute.value = null
    }

    val askPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        container.scope.launch { container.changed() }
    }
    LaunchedEffect(settings.onboarded) {
        if (settings.onboarded && Build.VERSION.SDK_INT >= 33 && !Alarms.canNotify(context) && !BuildConfig.DEMO) {
            askPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    CompositionLocalProvider(LocalPerson provides Person(settings.name, settings.gender)) {
        Backdrop(Modifier.fillMaxSize()) {
            NavHost(
                navController = nav,
                startDestination = start,
                enterTransition = { fadeIn(tween(240)) },
                exitTransition = { fadeOut(tween(180)) },
                popEnterTransition = { fadeIn(tween(240)) },
                popExitTransition = { fadeOut(tween(180)) },
            ) {
                composable("welcome") {
                    OnboardingScreen(data) { openLibrary ->
                        nav.navigate("main") { popUpTo("welcome") { inclusive = true } }
                        if (openLibrary) go.library()
                    }
                }
                composable("main") { MainTabs(tab, { tab = it }, go, data) }
                composable("sphere/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
                    SphereScreen(e.arguments?.getLong("id") ?: 0L, data, go)
                }
                composable("sphereForm/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
                    SphereFormScreen(e.arguments?.getLong("id") ?: 0L, data, go)
                }
                composable("role/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
                    RoleScreen(e.arguments?.getLong("id") ?: 0L, data, go)
                }
                composable(
                    "roleForm?id={id}&sphere={sphere}&shadow={shadow}",
                    arguments = listOf(
                        navArgument("id") { type = NavType.LongType; defaultValue = 0L },
                        navArgument("sphere") { type = NavType.LongType; defaultValue = 0L },
                        navArgument("shadow") { type = NavType.BoolType; defaultValue = false },
                    ),
                ) { e ->
                    val a = e.arguments
                    RoleFormScreen(a?.getLong("id") ?: 0L, a?.getLong("sphere") ?: 0L, a?.getBoolean("shadow") ?: false, data, go)
                }
                composable(
                    "library?sphere={sphere}&shadows={shadows}",
                    arguments = listOf(
                        navArgument("sphere") { type = NavType.StringType; defaultValue = "" },
                        navArgument("shadows") { type = NavType.BoolType; defaultValue = false },
                    ),
                ) { e ->
                    LibraryScreen(e.arguments?.getString("sphere").orEmpty(), e.arguments?.getBoolean("shadows") ?: false, data, go)
                }
                composable("preset/{key}", arguments = listOf(navArgument("key") { type = NavType.StringType })) { e ->
                    PresetScreen(e.arguments?.getString("key").orEmpty(), data, go)
                }
                composable("core") { CoreScreen(data, go) }
                composable("morning") { MorningScreen(data, go) }
                composable("review") { ReviewScreen(data, go) }
                composable("quick?role={role}", arguments = listOf(navArgument("role") { type = NavType.LongType; defaultValue = 0L })) { e ->
                    QuickSceneScreen(e.arguments?.getLong("role") ?: 0L, data, go)
                }
                composable("threshold/{role}", arguments = listOf(navArgument("role") { type = NavType.LongType })) { e ->
                    ThresholdScreen(e.arguments?.getLong("role") ?: 0L, data, go)
                }
                composable("thresholds") { ThresholdsScreen(data, go) }
                composable("sos") { SosScreen(data, go) }
                composable(
                    "rehearsal/{id}?role={role}",
                    arguments = listOf(navArgument("id") { type = NavType.LongType }, navArgument("role") { type = NavType.LongType; defaultValue = 0L }),
                ) { e ->
                    RehearsalScreen(e.arguments?.getLong("id") ?: 0L, e.arguments?.getLong("role") ?: 0L, data, go)
                }
                composable("council/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
                    CouncilScreen(e.arguments?.getLong("id") ?: 0L, data, go)
                }
                composable(
                    "note/{kind}/{id}?role={role}",
                    arguments = listOf(
                        navArgument("kind") { type = NavType.StringType },
                        navArgument("id") { type = NavType.LongType },
                        navArgument("role") { type = NavType.LongType; defaultValue = 0L },
                    ),
                ) { e ->
                    val kind = Practice.of(e.arguments?.getString("kind"))
                    if (kind != null) NoteScreen(kind, e.arguments?.getLong("id") ?: 0L, e.arguments?.getLong("role") ?: 0L, data, go)
                }
                composable("anchor/{role}", arguments = listOf(navArgument("role") { type = NavType.LongType })) { e ->
                    AnchorScreen(e.arguments?.getLong("role") ?: 0L, data, go)
                }
                composable(
                    "act/{id}?role={role}",
                    arguments = listOf(navArgument("id") { type = NavType.LongType }, navArgument("role") { type = NavType.LongType; defaultValue = 0L }),
                ) { e ->
                    ActScreen(e.arguments?.getLong("id") ?: 0L, e.arguments?.getLong("role") ?: 0L, data, go)
                }
                composable("settings") { SettingsScreen(data, go) }
            }
        }
    }
}

@Composable
private fun MainTabs(tab: Tab, onTab: (Tab) -> Unit, go: Go, data: WData) {
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) {
            when (tab) {
                Tab.PRISM -> PrismTab(data, go, onTab)
                Tab.ROLES -> RolesTab(data, go)
                Tab.PRACTICES -> PracticesTab(data, go)
                Tab.JOURNAL -> JournalTab(data, go)
            }
        }
        HorizontalDivider(color = U.c.line)
        Row(
            Modifier.fillMaxWidth().background(U.c.bg.copy(alpha = 0.94f)).navigationBarsPadding().height(66.dp).padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NavItem(Icons.Rounded.Lightbulb, stringResource(R.string.tab_prism), tab == Tab.PRISM) { onTab(Tab.PRISM) }
            NavItem(Icons.Rounded.Diamond, stringResource(R.string.tab_roles), tab == Tab.ROLES) { onTab(Tab.ROLES) }
            NavItem(Icons.Rounded.SelfImprovement, stringResource(R.string.tab_practices), tab == Tab.PRACTICES) { onTab(Tab.PRACTICES) }
            NavItem(Icons.AutoMirrored.Rounded.MenuBook, stringResource(R.string.tab_journal), tab == Tab.JOURNAL) { onTab(Tab.JOURNAL) }
        }
    }
}

@Composable
private fun RowScope.NavItem(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    val color = if (selected) U.c.accent else U.c.faint
    Column(
        Modifier
            .weight(1f)
            .clip(RoundedCornerShape(14.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.clip(RoundedCornerShape(50)).background(if (selected) U.c.accentSoft else Color.Transparent).padding(horizontal = 18.dp, vertical = 4.dp),
        ) { Icon(icon, null, Modifier.size(22.dp), tint = color) }
        Spacer(Modifier.height(3.dp))
        Text(label, fontFamily = Inter, fontSize = 11.5.sp, color = color, maxLines = 1)
    }
}
