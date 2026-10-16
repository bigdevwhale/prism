package app.prism.domain

import app.prism.data.Act
import app.prism.data.ActDay
import app.prism.data.CoreValue
import app.prism.data.DayPlan
import app.prism.data.Note
import app.prism.data.Rehearsal
import app.prism.data.Role
import app.prism.data.Scene
import app.prism.data.Sphere
import app.prism.data.Threshold
import java.time.LocalDate

/** How a role has been played: scenes, ratings, energy and the points that polish its facet. */
class RoleStats(
    val scenes: Int,
    val rated: Int,
    /** 0..100, null when nothing is rated. */
    val score: Float?,
    /** -2..2, null when nothing is rated. */
    val energy: Float?,
    val thresholds: Int,
    val points: Int,
    val lastDay: Long?,
    /** For a shadow: how often it showed up. */
    val appearances: Int,
) {
    val cut: Cut get() = Cut.of(points)

    companion object {
        val EMPTY = RoleStats(0, 0, null, null, 0, 0, null, 0)
    }
}

/** One coloured ray of the prism: a role and how it shone over a stretch of days. */
class Ray(val role: Role, val hue: Int, val scenes: Int, val score: Float?)

/** Everything stored, as one snapshot for a given day. */
class World(
    val core: List<CoreValue>,
    val spheres: List<Sphere>,
    allRoles: List<Role>,
    val scenes: List<Scene>,
    days: List<DayPlan>,
    val rehearsals: List<Rehearsal>,
    val thresholds: List<Threshold>,
    val acts: List<Act>,
    actDays: List<ActDay>,
    val notes: List<Note>,
    val today: LocalDate,
) {
    val day: Long = today.toEpochDay()

    private val sphereById = spheres.associateBy { it.id }
    private val roleById = allRoles.associateBy { it.id }
    private val planByDay = days.associateBy { it.day }
    private val actDaySet = actDays.map { it.actId to it.day }.toHashSet()
    private val actDayCount = actDays.groupingBy { it.actId }.eachCount()

    /** Roles proper, in sphere order. */
    val roles: List<Role> = allRoles.filter { !it.shadow }
        .sortedWith(compareBy({ sphereById[it.sphereId]?.sort ?: Int.MAX_VALUE }, { it.sphereId }, { it.sort }, { it.id }))
    val shadows: List<Role> = allRoles.filter { it.shadow }

    fun sphere(id: Long): Sphere? = sphereById[id]
    fun role(id: Long): Role? = roleById[id]
    fun rolesIn(sphereId: Long): List<Role> = roles.filter { it.sphereId == sphereId }
    fun shadowsIn(sphereId: Long): List<Role> = shadows.filter { it.sphereId == sphereId }
    fun sphereByPreset(key: String): Sphere? = spheres.firstOrNull { it.presetKey == key }
    fun hasPreset(presetKey: String): Boolean = roleById.values.any { it.presetKey == presetKey }

    /** The role's own colour, or its sphere's. */
    fun hueOf(role: Role): Int = if (role.hue >= 0) role.hue else sphere(role.sphereId)?.hue ?: 0

    fun plan(day: Long = this.day): DayPlan? = planByDay[day]
    val focus: Role? get() = plan()?.focusRoleId?.let { role(it) }
    fun reviewed(day: Long = this.day): Boolean = plan(day)?.reviewed == true || scenes.any { it.day == day && it.kind == Scene.REVIEW }

    fun scenesOn(day: Long): List<Scene> = scenes.filter { it.day == day }
    fun scenesOf(roleId: Long): List<Scene> = scenes.filter { it.roleId == roleId }

    private val statsCache = HashMap<Long, RoleStats>()

    fun stats(roleId: Long): RoleStats = statsCache.getOrPut(roleId) {
        val mine = scenes.filter { it.roleId == roleId }
        val rated = mine.filter { it.score >= 0 }
        val thresholds = mine.count { it.kind == Scene.THRESHOLD }
        val appearances = scenes.count { it.shadowId == roleId } + mine.count { it.kind == Scene.SHADOW }
        val myActs = acts.filter { it.roleId == roleId }
        val points = rated.count { it.score >= 60 } + thresholds +
            myActs.sumOf { actDone(it) } + myActs.count { actCompleted(it) } * 10
        RoleStats(
            scenes = mine.size,
            rated = rated.size,
            score = rated.takeIf { it.isNotEmpty() }?.map { it.score }?.average()?.toFloat(),
            energy = rated.takeIf { it.isNotEmpty() }?.map { it.energy }?.average()?.toFloat(),
            thresholds = thresholds,
            points = points,
            lastDay = mine.maxOfOrNull { it.day },
            appearances = appearances,
        )
    }

    /** Rays of every role played between [from] and [to] (inclusive), in sphere order; unplayed roles get a thin ray. */
    fun rays(from: Long, to: Long = day, includeIdle: Boolean = true): List<Ray> {
        val window = scenes.filter { it.day in from..to }
        return roles.mapNotNull { r ->
            val mine = window.filter { it.roleId == r.id }
            val rated = mine.filter { it.score >= 0 }
            if (mine.isEmpty() && !includeIdle) null
            else Ray(r, hueOf(r), mine.size, rated.takeIf { it.isNotEmpty() }?.map { it.score }?.average()?.toFloat())
        }
    }

    /** Scenes per sphere between [from] and today: where life actually goes. */
    fun balance(from: Long): List<Pair<Sphere, Int>> {
        val window = scenes.filter { it.day >= from && it.kind != Scene.SHADOW }
        return spheres.map { s -> s to window.count { sc -> role(sc.roleId)?.let { it.sphereId == s.id && !it.shadow } == true } }
    }

    /** How often each shadow showed up since [from], most frequent first. */
    fun shadowCounts(from: Long): List<Pair<Role, Int>> = shadows.map { sh ->
        sh to scenes.count { it.day >= from && (it.shadowId == sh.id || (it.kind == Scene.SHADOW && it.roleId == sh.id)) }
    }.sortedByDescending { it.second }

    /** Average energy per role over everything rated: what charges and what drains. */
    fun energyMap(): List<Pair<Role, Float>> = roles.mapNotNull { r -> stats(r.id).energy?.let { r to it } }.sortedByDescending { it.second }

    // Acts.

    fun actDone(act: Act): Int = actDayCount[act.id] ?: 0
    fun actDoneOn(act: Act, day: Long): Boolean = (act.id to day) in actDaySet
    fun actLastDay(act: Act): Long = act.startDay + act.length - 1
    fun actRunning(act: Act): Boolean = act.endedAt == null && day in act.startDay..actLastDay(act)
    /** An act counts as finished when two thirds of its days were done. */
    fun actCompleted(act: Act): Boolean = actDone(act) * 3 >= act.length * 2 && (act.endedAt != null || day > actLastDay(act))
    val runningActs: List<Act> get() = acts.filter { actRunning(it) }
    fun actOf(roleId: Long): Act? = runningActs.firstOrNull { it.roleId == roleId }
    /** 0-based day of the act that [day] is. */
    fun actIndex(act: Act, day: Long = this.day): Int = (day - act.startDay).toInt()

    // Rehearsals.

    val upcoming: List<Rehearsal> get() = rehearsals.filter { !it.done }.sortedWith(compareBy(nullsLast()) { it.day })
    val rehearsalToday: Rehearsal? get() = upcoming.firstOrNull { it.day == day }

    fun notesOf(kind: Practice): List<Note> = notes.filter { it.kind == kind.name }

    /** Days in a row, ending today or yesterday, with at least one scene. */
    val streak: Int
        get() {
            val days = scenes.map { it.day }.toHashSet()
            var d = if (day in days) day else day - 1
            var n = 0
            while (d in days) { n++; d-- }
            return n
        }
}
