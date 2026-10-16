package app.prism

import android.app.Application
import android.content.Context
import app.prism.data.DemoSeed
import app.prism.data.PrismDao
import app.prism.data.PrismDatabase
import app.prism.data.SettingsStore
import app.prism.notify.Alarms
import app.prism.widget.RoleWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppContainer(private val context: Context) {
    val db = PrismDatabase.create(context)
    val dao: PrismDao get() = db.dao()
    val settings = SettingsStore(context)
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Fire-and-forget so a write survives the screen being closed right after tapping save. */
    fun write(block: suspend PrismDao.() -> Unit) {
        scope.launch {
            dao.block()
            changed()
        }
    }

    /** Re-arm reminders and redraw the home-screen widget. */
    suspend fun changed() {
        Alarms.reschedule(context)
        RoleWidget.refresh(context)
    }
}

class PrismApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Alarms.ensureChannels(this)
        // Re-arm on every start: OEM task killers (HyperOS & co.) silently drop alarms.
        container.scope.launch {
            if (BuildConfig.DEMO) DemoSeed.seedIfEmpty(this@PrismApp, container.dao, container.settings)
            container.changed()
        }
    }
}

val Context.container: AppContainer get() = (applicationContext as PrismApp).container
