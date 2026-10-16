package app.prism.domain

/** How Russian text addresses the user: "ты спокойный" or "ты спокойная". English ignores it. */
enum class Gender { MALE, FEMALE;
    companion object {
        fun of(id: String?): Gender = entries.firstOrNull { it.name == id } ?: FEMALE
    }
}

private val genderMark = Regex("\\{([^{}|]*)\\|([^{}|]*)\\}")

/** Resolves "{спокойный|спокойная}" marks: the first form for [Gender.MALE], the second for [Gender.FEMALE]. */
fun gendered(text: String, gender: Gender): String =
    if ('|' !in text) text else genderMark.replace(text) { m -> m.groupValues[if (gender == Gender.MALE) 1 else 2] }

/**
 * Everything the Practices tab offers. Written ones save a [app.prism.data.Note] of their kind;
 * the rest save scenes, rehearsals or acts.
 */
enum class Practice(val written: Boolean = false, val needsRole: Boolean = false) {
    MORNING,
    THRESHOLD(needsRole = true),
    REHEARSAL,
    SOS,
    REVIEW,
    COUNCIL(written = true),
    LETTER(written = true, needsRole = true),
    OTHERS(written = true),
    SPEECH(written = true),
    SHADOW_TALK(written = true),
    MODEL(written = true, needsRole = true),
    ANCHOR(needsRole = true),
    ACT(needsRole = true);

    companion object {
        fun of(name: String?): Practice? = entries.firstOrNull { it.name == name }
    }
}

/** How polished a facet is, from the scenes played well in it and the acts finished. */
enum class Cut(val points: Int) {
    SKETCH(0), FACET(5), POLISHED(20), BRILLIANT(50);

    companion object {
        fun of(points: Int): Cut = entries.last { points >= it.points }

        /** 0..1 progress from this cut to the next one. */
        fun progress(points: Int): Float {
            val c = of(points)
            val next = entries.getOrNull(c.ordinal + 1) ?: return 1f
            return (points - c.points).toFloat() / (next.points - c.points)
        }
    }
}

/** Splits text into its non-blank lines. */
fun lines(text: String): List<String> = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
