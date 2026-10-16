package app.prism.content

import java.util.Locale

/**
 * The ready-made library: spheres of life, good roles for each, and the shadows people slip into.
 * English and Russian live in separate files with the same keys; [app.prism.LibraryTest] checks they match.
 * Russian text may hold "{м|ж}" marks, resolved by [app.prism.domain.gendered].
 */
class SphereText(
    val name: String,
    /** One line: what this part of life is about. */
    val blurb: String,
)

class RoleText(
    /** "<sphere>.<slug>", stable forever: saved roles remember which preset they came from. */
    val key: String,
    val sphere: String,
    /** An archetype, 1–3 words: "Quiet Harbor". */
    val name: String,
    /** One sentence: what people get when you are in this role. */
    val tagline: String,
    /** Exactly three. */
    val qualities: List<String>,
    /** How this role speaks. */
    val voice: String,
    /** Three concrete things this role does. */
    val does: List<String>,
    /** Two things this role never does. */
    val never: List<String>,
    /** Posture and breath. */
    val body: String,
    /** A short line that switches the role on. */
    val motto: String,
    /** One small daily practice. */
    val practice: String,
    /** Seven one-day tasks for a focus act, cycled through its days. */
    val tasks: List<String>,
)

class ShadowText(
    /** "shadow.<slug>". */
    val key: String,
    /** A character name: "The Prosecutor". */
    val name: String,
    /** What it does, one sentence. */
    val tagline: String,
    /** When it shows up. */
    val triggers: String,
    /** The good thing it is clumsily trying to protect. */
    val protects: String,
    /** What is really needed underneath. */
    val needs: String,
    /** How to notice it early: body, voice, words. */
    val signs: String,
    /** What to do instead when it shows up. */
    val answer: String,
    /** Sphere keys whose roles are a good answer to it. */
    val answerSpheres: List<String>,
)

class Library(
    val spheres: Map<String, SphereText>,
    val roles: List<RoleText>,
    val shadows: List<ShadowText>,
) {
    private val byKey = roles.associateBy { it.key }
    private val shadowByKey = shadows.associateBy { it.key }

    fun role(key: String?): RoleText? = key?.let { byKey[it] }
    fun shadow(key: String?): ShadowText? = key?.let { shadowByKey[it] }
    fun rolesOf(sphere: String): List<RoleText> = roles.filter { it.sphere == sphere }

    companion object {
        val En by lazy { Library(SpheresEn, rolesEn, ShadowsEn) }
        val Ru by lazy { Library(SpheresRu, rolesRu, ShadowsRu) }

        fun of(locale: Locale): Library = if (locale.language == "ru") Ru else En
    }
}

private val rolesEn get() = RolesEnA + RolesEnB + RolesEnC + RolesEnD
private val rolesRu get() = RolesRuA + RolesRuB + RolesRuC + RolesRuD
