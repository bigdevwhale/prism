package app.prism.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** One value of the core: the white light every role is made of. */
@Entity(tableName = "core")
data class CoreValue(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val sort: Int = 0,
)

/** A part of life. A preset sphere has [presetKey] and an empty [name] (shown in the app's language); a custom one has a name. */
@Entity(tableName = "spheres")
data class Sphere(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val presetKey: String? = null,
    val name: String = "",
    /** [app.prism.ui.SphereIcon] name. */
    val icon: String,
    /** Index into the spectrum. */
    val hue: Int,
    val sort: Int = 0,
)

/**
 * A role the user plays, or a shadow they slip into ([shadow]). A role adopted from the library keeps its
 * [presetKey] and a copy of the text, which the user is free to rewrite. Lists are stored one item per line.
 */
@Entity(tableName = "roles", indices = [Index("sphereId")])
data class Role(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** 0 for a shadow that belongs to no sphere. */
    val sphereId: Long,
    val presetKey: String? = null,
    val shadow: Boolean = false,
    val name: String,
    val tagline: String = "",
    val qualities: String = "",
    val voice: String = "",
    val does: String = "",
    val never: String = "",
    val body: String = "",
    val motto: String = "",
    val practice: String = "",
    /** A gesture, word or memory that switches the role on. */
    val anchor: String = "",
    /** Someone who plays this role well: a person to borrow from. */
    val model: String = "",
    /** Spectrum index; -1 takes the sphere's colour. */
    val hue: Int = -1,
    // Shadow fields.
    val triggers: String = "",
    val protects: String = "",
    val needs: String = "",
    val signs: String = "",
    val answer: String = "",
    /** For a shadow: the role that answers it, or 0. */
    val antidoteId: Long = 0,
    val createdAt: Long,
    val sort: Int = 0,
)

/**
 * A scene played in a role: rated in the evening review or right after, or a threshold crossed into it.
 * [score] is 0..100 (how close to the role as written), -1 when not rated; [energy] is -2..2.
 */
@Entity(tableName = "scenes", indices = [Index("roleId"), Index("day")])
data class Scene(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val day: Long,
    val at: Long,
    val roleId: Long,
    val kind: Int = REVIEW,
    val score: Int = -1,
    val energy: Int = 0,
    /** The shadow that showed up in this scene, or 0. */
    val shadowId: Long = 0,
    val note: String = "",
) {
    companion object {
        const val REVIEW = 0
        const val QUICK = 1
        const val THRESHOLD = 2
        const val SOS = 3
        /** A shadow noticed on its own, outside a rated scene: roleId is the shadow. */
        const val SHADOW = 4
    }
}

/** The morning choice: which role leads today and with what intention. */
@Entity(tableName = "days")
data class DayPlan(
    @PrimaryKey val day: Long,
    val focusRoleId: Long = 0,
    val intention: String = "",
    val reviewed: Boolean = false,
)

/** Preparing a hard conversation: the role to be in, what to say, what not to say, and how it went. */
@Entity(tableName = "rehearsals")
data class Rehearsal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val roleId: Long,
    val title: String,
    /** Epoch day of the conversation, if known. */
    val day: Long? = null,
    val goal: String = "",
    val say: String = "",
    val notSay: String = "",
    /** The line to step out with if it goes badly. */
    val exit: String = "",
    val outcome: String = "",
    val outcomeScore: Int = -1,
    val done: Boolean = false,
    val createdAt: Long,
)

/** A scheduled crossing into a role: "on weekdays at 18:30, step into Quiet Harbor". */
@Entity(tableName = "thresholds")
data class Threshold(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val roleId: Long,
    /** Minutes after midnight. */
    val at: Int,
    /** Monday = bit 0. */
    val days: Int = 0b1111111,
    val on: Boolean = true,
    /** Shown in the reminder: "Coming home", "Before the team call". */
    val label: String = "",
)

/** A focus act: a stretch of days spent growing one role, one small task a day. */
@Entity(tableName = "acts")
data class Act(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val roleId: Long,
    val startDay: Long,
    val length: Int = 21,
    /** Set when finished or dropped early. */
    val endedAt: Long? = null,
    /** The user's own tasks, one per line; empty uses the preset's. */
    val tasks: String = "",
)

@Entity(tableName = "act_days", primaryKeys = ["actId", "day"])
data class ActDay(
    val actId: Long,
    val day: Long,
    val at: Long,
)

/**
 * A written practice: a letter from a role, how others see you, the speech at eighty, a talk with a shadow,
 * a council of roles… [kind] is a [app.prism.domain.Practice] name, [body] is JSON owned by that practice.
 */
@Entity(tableName = "notes", indices = [Index("roleId")])
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kind: String,
    val roleId: Long = 0,
    val title: String = "",
    val body: String = "",
    val day: Long,
    val createdAt: Long,
)

@Dao
interface PrismDao {
    @Query("SELECT * FROM core ORDER BY sort, id") fun observeCore(): Flow<List<CoreValue>>
    @Query("SELECT * FROM spheres ORDER BY sort, id") fun observeSpheres(): Flow<List<Sphere>>
    @Query("SELECT * FROM roles ORDER BY sort, id") fun observeRoles(): Flow<List<Role>>
    @Query("SELECT * FROM scenes ORDER BY at DESC") fun observeScenes(): Flow<List<Scene>>
    @Query("SELECT * FROM days") fun observeDays(): Flow<List<DayPlan>>
    @Query("SELECT * FROM rehearsals ORDER BY createdAt DESC") fun observeRehearsals(): Flow<List<Rehearsal>>
    @Query("SELECT * FROM thresholds ORDER BY at") fun observeThresholds(): Flow<List<Threshold>>
    @Query("SELECT * FROM acts ORDER BY startDay DESC") fun observeActs(): Flow<List<Act>>
    @Query("SELECT * FROM act_days") fun observeActDays(): Flow<List<ActDay>>
    @Query("SELECT * FROM notes ORDER BY createdAt DESC") fun observeNotes(): Flow<List<Note>>

    @Query("SELECT * FROM core ORDER BY sort, id") suspend fun core(): List<CoreValue>
    @Query("SELECT * FROM spheres ORDER BY sort, id") suspend fun spheres(): List<Sphere>
    @Query("SELECT * FROM roles ORDER BY sort, id") suspend fun roles(): List<Role>
    @Query("SELECT * FROM scenes ORDER BY at DESC") suspend fun scenes(): List<Scene>
    @Query("SELECT * FROM days") suspend fun days(): List<DayPlan>
    @Query("SELECT * FROM rehearsals ORDER BY createdAt DESC") suspend fun rehearsals(): List<Rehearsal>
    @Query("SELECT * FROM thresholds ORDER BY at") suspend fun thresholds(): List<Threshold>
    @Query("SELECT * FROM acts ORDER BY startDay DESC") suspend fun acts(): List<Act>
    @Query("SELECT * FROM act_days") suspend fun actDays(): List<ActDay>
    @Query("SELECT * FROM notes ORDER BY createdAt DESC") suspend fun notes(): List<Note>

    @Upsert suspend fun upsertCore(v: CoreValue): Long
    @Query("DELETE FROM core WHERE id = :id") suspend fun deleteCore(id: Long)
    @Query("DELETE FROM core") suspend fun clearCore()

    @Upsert suspend fun upsertSphere(s: Sphere): Long
    @Query("DELETE FROM spheres WHERE id = :id") suspend fun deleteSphereRow(id: Long)

    @Upsert suspend fun upsertRole(r: Role): Long
    @Query("DELETE FROM roles WHERE id = :id") suspend fun deleteRoleRow(id: Long)
    @Query("SELECT * FROM roles WHERE id = :id") suspend fun role(id: Long): Role?

    @Insert suspend fun insertScene(s: Scene): Long
    @Upsert suspend fun upsertScene(s: Scene): Long
    @Query("DELETE FROM scenes WHERE id = :id") suspend fun deleteScene(id: Long)
    @Query("DELETE FROM scenes WHERE day = :day AND kind = 0") suspend fun clearReview(day: Long)

    @Upsert suspend fun upsertDay(d: DayPlan)
    @Query("SELECT * FROM days WHERE day = :day") suspend fun day(day: Long): DayPlan?

    @Upsert suspend fun upsertRehearsal(r: Rehearsal): Long
    @Query("DELETE FROM rehearsals WHERE id = :id") suspend fun deleteRehearsal(id: Long)

    @Upsert suspend fun upsertThreshold(t: Threshold): Long
    @Query("DELETE FROM thresholds WHERE id = :id") suspend fun deleteThreshold(id: Long)

    @Upsert suspend fun upsertAct(a: Act): Long
    @Query("DELETE FROM acts WHERE id = :id") suspend fun deleteActRow(id: Long)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertActDay(d: ActDay)
    @Query("DELETE FROM act_days WHERE actId = :actId AND day = :day") suspend fun deleteActDay(actId: Long, day: Long)

    @Upsert suspend fun upsertNote(n: Note): Long
    @Query("DELETE FROM notes WHERE id = :id") suspend fun deleteNote(id: Long)

    // Cascades by hand: rows hang off roles and spheres by id.
    @Query("DELETE FROM scenes WHERE roleId = :id") suspend fun deleteScenesOf(id: Long)
    @Query("UPDATE scenes SET shadowId = 0 WHERE shadowId = :id") suspend fun unlinkShadow(id: Long)
    @Query("UPDATE roles SET antidoteId = 0 WHERE antidoteId = :id") suspend fun unlinkAntidote(id: Long)
    @Query("DELETE FROM rehearsals WHERE roleId = :id") suspend fun deleteRehearsalsOf(id: Long)
    @Query("DELETE FROM thresholds WHERE roleId = :id") suspend fun deleteThresholdsOf(id: Long)
    @Query("DELETE FROM act_days WHERE actId IN (SELECT id FROM acts WHERE roleId = :id)") suspend fun deleteActDaysOf(id: Long)
    @Query("DELETE FROM acts WHERE roleId = :id") suspend fun deleteActsOf(id: Long)
    @Query("UPDATE notes SET roleId = 0 WHERE roleId = :id") suspend fun unlinkNotes(id: Long)
    @Query("UPDATE days SET focusRoleId = 0 WHERE focusRoleId = :id") suspend fun unlinkFocus(id: Long)
    @Query("SELECT id FROM roles WHERE sphereId = :sphereId") suspend fun roleIdsOf(sphereId: Long): List<Long>
    @Query("DELETE FROM act_days WHERE actId = :id") suspend fun deleteActDaysOfAct(id: Long)

    @Query("DELETE FROM core") suspend fun wipeCore()
    @Query("DELETE FROM spheres") suspend fun wipeSpheres()
    @Query("DELETE FROM roles") suspend fun wipeRoles()
    @Query("DELETE FROM scenes") suspend fun wipeScenes()
    @Query("DELETE FROM days") suspend fun wipeDays()
    @Query("DELETE FROM rehearsals") suspend fun wipeRehearsals()
    @Query("DELETE FROM thresholds") suspend fun wipeThresholds()
    @Query("DELETE FROM acts") suspend fun wipeActs()
    @Query("DELETE FROM act_days") suspend fun wipeActDays()
    @Query("DELETE FROM notes") suspend fun wipeNotes()
}

@Database(
    entities = [CoreValue::class, Sphere::class, Role::class, Scene::class, DayPlan::class, Rehearsal::class, Threshold::class, Act::class, ActDay::class, Note::class],
    version = 1,
    exportSchema = true,
)
abstract class PrismDatabase : RoomDatabase() {
    abstract fun dao(): PrismDao

    companion object {
        fun create(context: Context): PrismDatabase =
            Room.databaseBuilder(context.applicationContext, PrismDatabase::class.java, "prism.db").build()
    }
}
