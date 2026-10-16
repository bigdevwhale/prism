package app.prism

import app.prism.content.Library
import app.prism.content.SpherePresets
import app.prism.content.SpheresEn
import app.prism.content.SpheresRu
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryTest {
    private val marks = Regex("""\{[^{}|]*\|[^{}|]*\}""")

    @Test fun languagesMatch() {
        val en = Library.En
        val ru = Library.Ru
        assertEquals(en.roles.map { it.key }, ru.roles.map { it.key })
        assertEquals(en.shadows.map { it.key }, ru.shadows.map { it.key })
        assertEquals(SpheresEn.keys, SpheresRu.keys)
        assertEquals(SpherePresets.map { it.key }.toSet(), SpheresEn.keys)
    }

    @Test fun rolesAreComplete() {
        listOf(Library.En, Library.Ru).forEach { lib ->
            assertEquals(lib.roles.size, lib.roles.map { it.key }.toSet().size)
            lib.roles.forEach { r ->
                assertTrue(r.key, r.key.startsWith(r.sphere + "."))
                assertTrue(r.key, r.sphere in SpheresEn)
                assertEquals(r.key, 3, r.qualities.size)
                assertEquals(r.key, 3, r.does.size)
                assertEquals(r.key, 2, r.never.size)
                assertEquals(r.key, 7, r.tasks.size)
                assertTrue(r.key, r.name.isNotBlank() && r.motto.isNotBlank() && r.practice.isNotBlank())
            }
            SpheresEn.keys.forEach { s -> assertTrue(s, lib.rolesOf(s).size >= 3) }
            lib.shadows.forEach { s -> s.answerSpheres.forEach { assertTrue(s.key + " " + it, it in SpheresEn) } }
        }
    }

    @Test fun englishHasNoGenderMarks() {
        val en = Library.En
        val all = en.roles.flatMap { listOf(it.name, it.tagline, it.voice, it.body, it.motto, it.practice) + it.qualities + it.does + it.never + it.tasks } +
            en.shadows.flatMap { listOf(it.name, it.tagline, it.triggers, it.protects, it.needs, it.signs, it.answer) }
        all.forEach { assertTrue(it, !marks.containsMatchIn(it)) }
    }
}
