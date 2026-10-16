package app.prism.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import app.prism.MainActivity
import app.prism.R
import app.prism.container
import app.prism.notify.Alarms
import app.prism.ui.quote
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Today's role on the home screen: its name and motto. Tapping opens its threshold, or the morning choice. */
class RoleWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val pending = goAsync()
        scope.launch {
            try {
                refresh(context)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        suspend fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, RoleWidget::class.java))
            if (ids.isEmpty()) return
            val dao = context.container.dao
            val plan = dao.day(LocalDate.now().toEpochDay())
            val role = plan?.focusRoleId?.takeIf { it != 0L }?.let { dao.role(it) }
            val views = RemoteViews(context.packageName, R.layout.widget_role)
            views.setTextViewText(R.id.label, context.getString(R.string.widget_name))
            views.setTextViewText(R.id.role, role?.name ?: "✦")
            views.setTextViewText(
                R.id.motto,
                when {
                    role == null -> context.getString(R.string.widget_none)
                    plan.intention.isNotBlank() -> plan.intention
                    role.motto.isNotBlank() -> quote(role.motto, context.resources.configuration.locales[0])
                    else -> role.tagline
                },
            )
            views.setOnClickPendingIntent(
                R.id.root,
                PendingIntent.getActivity(
                    context, 5000,
                    Intent(context, MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        .putExtra(Alarms.EXTRA_ROUTE, if (role != null) "threshold/${role.id}" else "morning"),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ),
            )
            manager.updateAppWidget(ids, views)
        }
    }
}
