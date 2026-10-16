package app.prism.ui

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Anchor
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.ChildCare
import androidx.compose.material.icons.rounded.Diversity3
import androidx.compose.material.icons.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.Elderly
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FamilyRestroom
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Handshake
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocationCity
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Thunderstorm
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.material.icons.rounded.WavingHand
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Park
import androidx.compose.material.icons.rounded.Church
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.TheaterComedy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import app.prism.content.Library
import app.prism.data.Role
import app.prism.data.Sphere
import app.prism.domain.Gender
import app.prism.domain.gendered
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

/** Icons a sphere can wear; presets use the first group, custom spheres pick from all of them. */
enum class SphereIcon(val icon: ImageVector) {
    HOME(Icons.Rounded.Home),
    HEART(Icons.Rounded.Favorite),
    CHILD(Icons.Rounded.ChildCare),
    ELDER(Icons.Rounded.Elderly),
    FAMILY(Icons.Rounded.FamilyRestroom),
    FRIENDS(Icons.Rounded.Diversity3),
    WORK(Icons.Rounded.Work),
    FLAG(Icons.Rounded.Flag),
    HANDSHAKE(Icons.Rounded.Handshake),
    BOOK(Icons.AutoMirrored.Rounded.MenuBook),
    BRUSH(Icons.Rounded.Brush),
    RUN(Icons.Rounded.DirectionsRun),
    COIN(Icons.Rounded.Savings),
    SELF(Icons.Rounded.SelfImprovement),
    SPARK(Icons.Rounded.AutoAwesome),
    CITY(Icons.Rounded.LocationCity),
    WAVE(Icons.Rounded.WavingHand),
    STORM(Icons.Rounded.Thunderstorm),
    ANCHOR(Icons.Rounded.Anchor),
    PLAY(Icons.Rounded.SportsEsports),
    PHONE(Icons.Rounded.Smartphone),
    CARE(Icons.Rounded.VolunteerActivism),
    // Extra icons for custom spheres.
    PET(Icons.Rounded.Pets),
    MUSIC(Icons.Rounded.MusicNote),
    TRAVEL(Icons.Rounded.Flight),
    FOOD(Icons.Rounded.Restaurant),
    SCHOOL(Icons.Rounded.School),
    NATURE(Icons.Rounded.Park),
    FAITH(Icons.Rounded.Church),
    TEAM(Icons.Rounded.Groups),
    STAGE(Icons.Rounded.TheaterComedy),
    CALM(Icons.Rounded.Spa),
    TECH(Icons.Rounded.SmartToy),
    STAR(Icons.Rounded.Star);

    companion object {
        fun of(name: String?): SphereIcon = entries.firstOrNull { it.name == name } ?: STAR
    }
}

/** Who the app is talking to; Russian strings are gendered by it. */
class Person(val name: String, val gender: Gender)

val LocalPerson = staticCompositionLocalOf { Person("", Gender.FEMALE) }

/** The library in the app's current language. */
@Composable
@ReadOnlyComposable
fun library(): Library = Library.of(locale())

@Composable
@ReadOnlyComposable
fun locale(): Locale = LocalConfiguration.current.locales[0]

/** A string with gender marks resolved. */
@Composable
fun gs(@StringRes id: Int, vararg args: Any): String = gendered(stringResource(id, *args), LocalPerson.current.gender)

@Composable
fun gp(@PluralsRes id: Int, count: Int, vararg args: Any): String = gendered(pluralStringResource(id, count, *args), LocalPerson.current.gender)

/** Any text (library content, user text) with gender marks resolved. */
@Composable
fun g(text: String): String = gendered(text, LocalPerson.current.gender)

@Composable
fun sphereName(s: Sphere?): String {
    if (s == null) return ""
    if (s.name.isNotBlank() || s.presetKey == null) return s.name
    return g(library().spheres[s.presetKey]?.name.orEmpty())
}

@Composable
fun sphereBlurb(s: Sphere): String = s.presetKey?.let { g(library().spheres[it]?.blurb.orEmpty()) }.orEmpty()

fun sphereIcon(s: Sphere?): ImageVector = SphereIcon.of(s?.icon).icon

/** Role text as the user sees it: their own words, with any gender marks from the library resolved. */
@Composable
fun roleName(r: Role?): String = r?.let { g(it.name) }.orEmpty()

/** Short weekday name, Monday = 0. */
@Composable
fun weekdayShort(i: Int): String = DayOfWeek.of(i + 1).getDisplayName(TextStyle.SHORT_STANDALONE, locale()).replaceFirstChar { it.titlecase(locale()) }

/** A line in quotation marks of the language: «…» in Russian, “…” in English. */
fun quote(text: String, locale: Locale): String = if (locale.language == "ru") "«$text»" else "“$text”"

@Composable
fun quoted(text: String): String = quote(text, locale())

fun hhmm(minutes: Int): String = "%02d:%02d".format(minutes / 60, minutes % 60)
