package app.prism.notify

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.prism.MainActivity
import app.prism.R
import app.prism.container
import app.prism.data.Threshold
import app.prism.domain.gendered
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/** The moments Prism comes by on its own: choosing a role in the morning, the evening review, and scheduled thresholds. */
object Alarms {
    private const val CHANNEL = "roles_v1"
    const val EXTRA_ROUTE = "route"

    private const val ACTION_MORNING = "app.prism.notify.MORNING"
    private const val ACTION_EVENING = "app.prism.notify.EVENING"
    private const val ACTION_THRESHOLD = "app.prism.notify.THRESHOLD"

    private const val ID_MORNING = 1
    private const val ID_EVENING = 2

    fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun ensureChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, context.getString(R.string.channel_roles), NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = context.getString(R.string.channel_roles_desc)
                }
            )
        }
    }

    private fun pi(context: Context, action: String, code: Int) = PendingIntent.getBroadcast(
        context, code,
        Intent(context, AlarmReceiver::class.java).setAction(action),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    /** Next time the clock shows [minutes] after midnight, strictly after now. */
    fun nextAt(minutes: Int, now: LocalDateTime): LocalDateTime {
        val m = Math.floorMod(minutes, 24 * 60)
        val today = now.toLocalDate().atTime(m / 60, m % 60)
        return if (today.isAfter(now)) today else today.plusDays(1)
    }

    private fun ringsOn(t: Threshold, date: LocalDate): Boolean = t.on && t.days and (1 shl (date.dayOfWeek.value - 1)) != 0

    /** The soonest threshold after [now], within a week, with every threshold that rings at that minute. */
    fun nextThreshold(all: List<Threshold>, now: LocalDateTime): Pair<LocalDateTime, List<Threshold>>? {
        for (offset in 0..7) {
            val date = now.toLocalDate().plusDays(offset.toLong())
            val upcoming = all.filter { ringsOn(it, date) }
                .map { it to date.atTime(it.at / 60, it.at % 60) }
                .filter { it.second.isAfter(now) }
            val first = upcoming.minByOrNull { it.second } ?: continue
            return first.second to upcoming.filter { it.second == first.second }.map { it.first }
        }
        return null
    }

    suspend fun reschedule(context: Context) {
        val c = context.container
        val s = c.settings.now()
        val am = context.getSystemService(AlarmManager::class.java)
        val zone = ZoneId.systemDefault()
        val now = LocalDateTime.now(zone)
        fun set(action: String, code: Int, at: LocalDateTime?) {
            val p = pi(context, action, code)
            // Reminders don't need to-the-second precision; this avoids the exact-alarm permission.
            if (at == null || !s.onboarded) am.cancel(p)
            else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at.atZone(zone).toInstant().toEpochMilli(), p)
        }
        set(ACTION_MORNING, 1, if (s.morningOn) nextAt(s.morningAt, now) else null)
        set(ACTION_EVENING, 2, if (s.eveningOn) nextAt(s.eveningAt, now) else null)
        set(ACTION_THRESHOLD, 3, nextThreshold(c.dao.thresholds(), now)?.first)
    }

    suspend fun fire(context: Context, action: String) {
        val c = context.container
        val dao = c.dao
        val s = c.settings.now()
        val zone = ZoneId.systemDefault()
        val now = LocalDateTime.now(zone)
        val day = now.toLocalDate().toEpochDay()
        fun g(id: Int, vararg args: Any) = gendered(context.getString(id, *args), s.gender)
        when (action) {
            ACTION_MORNING -> if (s.morningOn && dao.day(day)?.focusRoleId.let { it == null || it == 0L }) {
                val talk = dao.rehearsals().firstOrNull { !it.done && it.day == day }
                val text = if (talk != null) g(R.string.notif_morning_talk, talk.title) else g(R.string.notif_morning_text)
                notify(context, ID_MORNING, g(R.string.notif_morning_title), text, "morning")
            }
            ACTION_EVENING -> if (s.eveningOn && dao.day(day)?.reviewed != true && dao.roles().any { !it.shadow }) {
                notify(context, ID_EVENING, g(R.string.notif_evening_title), g(R.string.notif_evening_text), "review")
            }
            ACTION_THRESHOLD -> {
                val all = dao.thresholds()
                // Everything due this minute (or missed in the last ten, if the phone dozed).
                val due = all.filter {
                    val at = now.toLocalDate().atTime(it.at / 60, it.at % 60)
                    ringsOn(it, now.toLocalDate()) && !at.isAfter(now) && at.isAfter(now.minusMinutes(10))
                }
                due.forEach { t ->
                    val role = dao.role(t.roleId) ?: return@forEach
                    val title = if (t.label.isNotBlank()) t.label else g(R.string.notif_threshold_title)
                    val text = g(R.string.notif_threshold_text, role.name) + if (role.motto.isNotBlank()) "\n«${role.motto}»" else ""
                    notify(context, (1000 + t.id % 100_000).toInt(), title, text, "threshold/${role.id}")
                }
            }
        }
    }

    private fun open(context: Context, code: Int, route: String) = PendingIntent.getActivity(
        context, code,
        Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(EXTRA_ROUTE, route),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun notify(context: Context, id: Int, title: String, text: String, route: String) {
        if (!canNotify(context)) return
        val b = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ContextCompat.getColor(context, R.color.accent))
            .setContentTitle(title)
            .setContentText(text.substringBefore('\n'))
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(open(context, 100 + id, route))
            .setAutoCancel(true)
        try {
            NotificationManagerCompat.from(context).notify(id, b.build())
        } catch (_: SecurityException) {
        }
    }

    internal val actions = setOf(ACTION_MORNING, ACTION_EVENING, ACTION_THRESHOLD)
}

private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action !in Alarms.actions) return
        val pending = goAsync()
        receiverScope.launch {
            try {
                Alarms.fire(context, action)
                context.container.changed()
            } finally {
                pending.finish()
            }
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    private val actions = setOf(
        Intent.ACTION_BOOT_COMPLETED,
        Intent.ACTION_MY_PACKAGE_REPLACED,
        Intent.ACTION_TIME_CHANGED,
        Intent.ACTION_TIMEZONE_CHANGED,
    )

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in actions) return
        val pending = goAsync()
        receiverScope.launch {
            try {
                context.container.changed()
            } finally {
                pending.finish()
            }
        }
    }
}
