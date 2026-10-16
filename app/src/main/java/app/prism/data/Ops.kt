package app.prism.data

import app.prism.content.Library
import app.prism.content.RoleText
import app.prism.content.ShadowText
import app.prism.content.SpherePresets

/** Operations that touch several tables. */

suspend fun PrismDao.deleteRole(id: Long) {
    deleteScenesOf(id)
    unlinkShadow(id)
    unlinkAntidote(id)
    deleteRehearsalsOf(id)
    deleteThresholdsOf(id)
    deleteActDaysOf(id)
    deleteActsOf(id)
    unlinkNotes(id)
    unlinkFocus(id)
    deleteRoleRow(id)
}

/** Removes a sphere with its roles; shadows in it stay, sphere-less. */
suspend fun PrismDao.deleteSphere(id: Long) {
    roles().filter { it.sphereId == id }.forEach { r ->
        if (r.shadow) upsertRole(r.copy(sphereId = 0)) else deleteRole(r.id)
    }
    deleteSphereRow(id)
}

suspend fun PrismDao.deleteAct(id: Long) {
    deleteActDaysOfAct(id)
    deleteActRow(id)
}

/** The sphere for a preset key, created at the end of the list if it isn't there yet. */
suspend fun PrismDao.ensurePresetSphere(key: String): Long {
    val all = spheres()
    all.firstOrNull { it.presetKey == key }?.let { return it.id }
    val p = SpherePresets.first { it.key == key }
    return upsertSphere(Sphere(presetKey = key, icon = p.icon, hue = p.hue, sort = (all.maxOfOrNull { it.sort } ?: -1) + 1))
}

/**
 * Copies a library role into the user's own roles, in its sphere (switched on if needed). Returns the new role id.
 * [fix] resolves gender marks, so the copy is plain text the user can edit.
 */
suspend fun PrismDao.adopt(t: RoleText, now: Long, fix: (String) -> String, sphereId: Long? = null): Long {
    val sid = sphereId ?: ensurePresetSphere(t.sphere)
    val sort = (roles().filter { it.sphereId == sid }.maxOfOrNull { it.sort } ?: -1) + 1
    return upsertRole(
        Role(
            sphereId = sid,
            presetKey = t.key,
            name = fix(t.name),
            tagline = fix(t.tagline),
            qualities = t.qualities.joinToString("\n", transform = fix),
            voice = fix(t.voice),
            does = t.does.joinToString("\n", transform = fix),
            never = t.never.joinToString("\n", transform = fix),
            body = fix(t.body),
            motto = fix(t.motto),
            practice = fix(t.practice),
            createdAt = now,
            sort = sort,
        )
    )
}

/** Copies a library shadow into the user's shadows. */
suspend fun PrismDao.adoptShadow(t: ShadowText, now: Long, fix: (String) -> String, sphereId: Long = 0): Long = upsertRole(
    Role(
        sphereId = sphereId,
        presetKey = t.key,
        shadow = true,
        name = fix(t.name),
        tagline = fix(t.tagline),
        triggers = fix(t.triggers),
        protects = fix(t.protects),
        needs = fix(t.needs),
        signs = fix(t.signs),
        answer = fix(t.answer),
        createdAt = now,
    )
)

/** Sets the spheres chosen in onboarding, in the given order, keeping any that already exist. */
suspend fun PrismDao.setupSpheres(keys: List<String>) {
    keys.forEach { ensurePresetSphere(it) }
}

/** Replaces the core with [values], in order. */
suspend fun PrismDao.setCore(values: List<String>) {
    clearCore()
    values.map { it.trim() }.filter { it.isNotEmpty() }.forEachIndexed { i, v -> upsertCore(CoreValue(text = v, sort = i)) }
}

/** The tasks of an act: the user's own, or the preset's, or the generic set. */
fun actTasks(act: Act, role: Role?, lib: Library, generic: List<String>): List<String> {
    val own = act.tasks.lines().map { it.trim() }.filter { it.isNotEmpty() }
    if (own.isNotEmpty()) return own
    return lib.role(role?.presetKey)?.tasks ?: generic
}
