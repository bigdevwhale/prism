package app.prism.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.prism.domain.Gender
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("settings")

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class Settings(
    /** Light through a prism reads best on the dark; paper is an option. */
    val theme: ThemeMode = ThemeMode.DARK,
    val onboarded: Boolean = false,
    val name: String = "",
    /** Only asked when the app speaks Russian. */
    val gender: Gender = Gender.FEMALE,
    /** "Which role leads today?" */
    val morningOn: Boolean = true,
    val morningAt: Int = 8 * 60 + 30,
    /** The evening review of scenes. */
    val eveningOn: Boolean = true,
    val eveningAt: Int = 21 * 60 + 30,
    val haptics: Boolean = true,
    /** Length of a threshold, in seconds. */
    val thresholdSeconds: Int = 60,
    /** Moments already shown once (a facet polished, an act finished). */
    val seen: Set<String> = emptySet(),
)

class SettingsStore(context: Context) {
    private val store = context.applicationContext.dataStore

    val settings: Flow<Settings> = store.data.map { it.read() }

    private fun Preferences.read() = Settings(
        theme = this[THEME]?.let { t -> runCatching { ThemeMode.valueOf(t) }.getOrNull() } ?: ThemeMode.DARK,
        onboarded = this[ONBOARDED] ?: false,
        name = this[NAME] ?: "",
        gender = Gender.of(this[GENDER]),
        morningOn = this[MORNING_ON] ?: true,
        morningAt = this[MORNING_AT] ?: (8 * 60 + 30),
        eveningOn = this[EVENING_ON] ?: true,
        eveningAt = this[EVENING_AT] ?: (21 * 60 + 30),
        haptics = this[HAPTICS] ?: true,
        thresholdSeconds = this[THRESHOLD_SECONDS] ?: 60,
        seen = this[SEEN] ?: emptySet(),
    )

    suspend fun now(): Settings = settings.first()

    suspend fun update(change: (Settings) -> Settings) {
        store.edit {
            val s = change(it.read())
            it[THEME] = s.theme.name
            it[ONBOARDED] = s.onboarded
            it[NAME] = s.name
            it[GENDER] = s.gender.name
            it[MORNING_ON] = s.morningOn
            it[MORNING_AT] = s.morningAt
            it[EVENING_ON] = s.eveningOn
            it[EVENING_AT] = s.eveningAt
            it[HAPTICS] = s.haptics
            it[THRESHOLD_SECONDS] = s.thresholdSeconds
            it[SEEN] = s.seen
        }
    }

    private companion object {
        val THEME = stringPreferencesKey("theme")
        val ONBOARDED = booleanPreferencesKey("onboarded")
        val NAME = stringPreferencesKey("name")
        val GENDER = stringPreferencesKey("gender")
        val MORNING_ON = booleanPreferencesKey("morning_on")
        val MORNING_AT = intPreferencesKey("morning_at")
        val EVENING_ON = booleanPreferencesKey("evening_on")
        val EVENING_AT = intPreferencesKey("evening_at")
        val HAPTICS = booleanPreferencesKey("haptics")
        val THRESHOLD_SECONDS = intPreferencesKey("threshold_seconds")
        val SEEN = stringSetPreferencesKey("seen")
    }
}
