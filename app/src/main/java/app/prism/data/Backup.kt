package app.prism.data

import androidx.room.withTransaction
import org.json.JSONArray
import org.json.JSONObject

/** Plain JSON backup: everything stays on the device unless the user exports it. */
object Backup {
    private const val VERSION = 1

    suspend fun export(db: PrismDatabase): String {
        val d = db.dao()
        return JSONObject()
            .put("app", "prism")
            .put("version", VERSION)
            .put("core", JSONArray(d.core().map { JSONObject().put("id", it.id).put("text", it.text).put("sort", it.sort) }))
            .put("spheres", JSONArray(d.spheres().map {
                JSONObject().put("id", it.id).put("presetKey", it.presetKey ?: JSONObject.NULL).put("name", it.name)
                    .put("icon", it.icon).put("hue", it.hue).put("sort", it.sort)
            }))
            .put("roles", JSONArray(d.roles().map {
                JSONObject().put("id", it.id).put("sphereId", it.sphereId).put("presetKey", it.presetKey ?: JSONObject.NULL)
                    .put("shadow", it.shadow).put("name", it.name).put("tagline", it.tagline).put("qualities", it.qualities)
                    .put("voice", it.voice).put("does", it.does).put("never", it.never).put("body", it.body).put("motto", it.motto)
                    .put("practice", it.practice).put("anchor", it.anchor).put("model", it.model).put("hue", it.hue)
                    .put("triggers", it.triggers).put("protects", it.protects).put("needs", it.needs).put("signs", it.signs)
                    .put("answer", it.answer).put("antidoteId", it.antidoteId).put("createdAt", it.createdAt).put("sort", it.sort)
            }))
            .put("scenes", JSONArray(d.scenes().map {
                JSONObject().put("id", it.id).put("day", it.day).put("at", it.at).put("roleId", it.roleId).put("kind", it.kind)
                    .put("score", it.score).put("energy", it.energy).put("shadowId", it.shadowId).put("note", it.note)
            }))
            .put("days", JSONArray(d.days().map {
                JSONObject().put("day", it.day).put("focusRoleId", it.focusRoleId).put("intention", it.intention).put("reviewed", it.reviewed)
            }))
            .put("rehearsals", JSONArray(d.rehearsals().map {
                JSONObject().put("id", it.id).put("roleId", it.roleId).put("title", it.title).put("day", it.day ?: JSONObject.NULL)
                    .put("goal", it.goal).put("say", it.say).put("notSay", it.notSay).put("exit", it.exit).put("outcome", it.outcome)
                    .put("outcomeScore", it.outcomeScore).put("done", it.done).put("createdAt", it.createdAt)
            }))
            .put("thresholds", JSONArray(d.thresholds().map {
                JSONObject().put("id", it.id).put("roleId", it.roleId).put("at", it.at).put("days", it.days).put("on", it.on).put("label", it.label)
            }))
            .put("acts", JSONArray(d.acts().map {
                JSONObject().put("id", it.id).put("roleId", it.roleId).put("startDay", it.startDay).put("length", it.length)
                    .put("endedAt", it.endedAt ?: JSONObject.NULL).put("tasks", it.tasks)
            }))
            .put("actDays", JSONArray(d.actDays().map { JSONObject().put("actId", it.actId).put("day", it.day).put("at", it.at) }))
            .put("notes", JSONArray(d.notes().map {
                JSONObject().put("id", it.id).put("kind", it.kind).put("roleId", it.roleId).put("title", it.title)
                    .put("body", it.body).put("day", it.day).put("createdAt", it.createdAt)
            }))
            .toString(1)
    }

    /** Replaces everything with the backup. Throws [IllegalArgumentException] when the text is not a Prism backup. */
    suspend fun import(db: PrismDatabase, text: String) {
        val root = runCatching { JSONObject(text) }.getOrNull()
        require(root != null && root.optString("app") == "prism") { "Not a Prism backup" }
        val core = root.list("core") { CoreValue(getLong("id"), getString("text"), optInt("sort")) }
        val spheres = root.list("spheres") {
            Sphere(getLong("id"), str("presetKey"), optString("name"), optString("icon"), optInt("hue"), optInt("sort"))
        }
        val roles = root.list("roles") {
            Role(
                id = getLong("id"), sphereId = optLong("sphereId"), presetKey = str("presetKey"), shadow = optBoolean("shadow"),
                name = getString("name"), tagline = optString("tagline"), qualities = optString("qualities"), voice = optString("voice"),
                does = optString("does"), never = optString("never"), body = optString("body"), motto = optString("motto"),
                practice = optString("practice"), anchor = optString("anchor"), model = optString("model"), hue = optInt("hue", -1),
                triggers = optString("triggers"), protects = optString("protects"), needs = optString("needs"), signs = optString("signs"),
                answer = optString("answer"), antidoteId = optLong("antidoteId"), createdAt = optLong("createdAt"), sort = optInt("sort"),
            )
        }
        val scenes = root.list("scenes") {
            Scene(getLong("id"), getLong("day"), optLong("at"), getLong("roleId"), optInt("kind"), optInt("score", -1), optInt("energy"), optLong("shadowId"), optString("note"))
        }
        val days = root.list("days") { DayPlan(getLong("day"), optLong("focusRoleId"), optString("intention"), optBoolean("reviewed")) }
        val rehearsals = root.list("rehearsals") {
            Rehearsal(
                getLong("id"), optLong("roleId"), optString("title"), if (isNull("day")) null else optLong("day"), optString("goal"),
                optString("say"), optString("notSay"), optString("exit"), optString("outcome"), optInt("outcomeScore", -1),
                optBoolean("done"), optLong("createdAt"),
            )
        }
        val thresholds = root.list("thresholds") {
            Threshold(getLong("id"), getLong("roleId"), getInt("at"), optInt("days", 0b1111111), optBoolean("on", true), optString("label"))
        }
        val acts = root.list("acts") {
            Act(getLong("id"), getLong("roleId"), getLong("startDay"), optInt("length", 21), if (isNull("endedAt")) null else optLong("endedAt"), optString("tasks"))
        }
        val actDays = root.list("actDays") { ActDay(getLong("actId"), getLong("day"), optLong("at")) }
        val notes = root.list("notes") {
            Note(getLong("id"), getString("kind"), optLong("roleId"), optString("title"), optString("body"), getLong("day"), optLong("createdAt"))
        }
        db.withTransaction {
            val d = db.dao()
            d.wipeCore(); d.wipeSpheres(); d.wipeRoles(); d.wipeScenes(); d.wipeDays()
            d.wipeRehearsals(); d.wipeThresholds(); d.wipeActs(); d.wipeActDays(); d.wipeNotes()
            core.forEach { d.upsertCore(it) }
            spheres.forEach { d.upsertSphere(it) }
            roles.forEach { d.upsertRole(it) }
            scenes.forEach { d.upsertScene(it) }
            days.forEach { d.upsertDay(it) }
            rehearsals.forEach { d.upsertRehearsal(it) }
            thresholds.forEach { d.upsertThreshold(it) }
            acts.forEach { d.upsertAct(it) }
            actDays.forEach { d.insertActDay(it) }
            notes.forEach { d.upsertNote(it) }
        }
    }

    private fun JSONObject.str(key: String): String? = if (isNull(key)) null else optString(key).takeIf { it.isNotEmpty() }

    private fun <T> JSONObject.list(key: String, read: JSONObject.() -> T): List<T> {
        val arr = optJSONArray(key) ?: return emptyList()
        return (0 until arr.length()).map { arr.getJSONObject(it).read() }
    }
}
