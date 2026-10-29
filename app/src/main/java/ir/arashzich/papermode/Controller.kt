package ir.arashzich.papermode

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.service.quicksettings.TileService
import java.util.Calendar

/** Single place that makes the saved choice real: apply when on, restore when off, keep the schedule armed. */
object Controller {

    fun setOn(c: Context, on: Boolean) {
        Store(c).on = on
        refresh(c)
    }

    fun toggle(c: Context) = setOn(c, !Store(c).on)

    /** With a schedule, "on" follows the clock; without one it is whatever the user chose. */
    fun sync(c: Context) {
        val s = Store(c)
        if (s.schedule) s.on = inWindow(s)
        refresh(c)
    }

    fun refresh(c: Context) {
        val s = Store(c)
        if (s.on) Tweaks.apply(c, s) else Tweaks.restore(c)
        arm(c, s)
        TileService.requestListeningState(c, ComponentName(c, ModeTile::class.java))
        val mgr = AppWidgetManager.getInstance(c)
        val ids = mgr.getAppWidgetIds(ComponentName(c, ModeWidget::class.java))
        if (ids.isNotEmpty()) ModeWidget.update(c, mgr, ids)
    }

    private fun minutesNow(): Int = Calendar.getInstance().let { it.get(Calendar.HOUR_OF_DAY) * 60 + it.get(Calendar.MINUTE) }

    fun inWindow(s: Store, m: Int = minutesNow()) =
        if (s.from <= s.to) m in s.from until s.to else m >= s.from || m < s.to

    private fun arm(c: Context, s: Store) {
        val am = c.getSystemService(AlarmManager::class.java)
        val pi = PendingIntent.getBroadcast(
            c, 0, Intent(c, AlarmReceiver::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        am.cancel(pi)
        if (!s.schedule) return
        val now = Calendar.getInstance()
        val next = listOf(s.from, s.to).minOf { minutes ->
            (now.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, minutes / 60); set(Calendar.MINUTE, minutes % 60)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                if (timeInMillis <= now.timeInMillis) add(Calendar.DAY_OF_YEAR, 1)
            }.timeInMillis
        }
        // Inexact on purpose: no extra permission, a few minutes of drift is fine here.
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next, pi)
    }
}

/** Boot, app update, clock or timezone change: re-apply and re-arm. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = Controller.sync(context)
}

/** Fires at the next schedule boundary. */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = Controller.sync(context)
}
