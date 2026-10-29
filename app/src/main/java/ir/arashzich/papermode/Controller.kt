package ir.arashzich.papermode

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.service.quicksettings.TileService
import java.util.Calendar

/** Single place that decides what should be active right now and makes it so. */
object Controller {

    fun setMode(c: Context, mode: Mode) {
        val s = Store(c)
        s.mode = mode
        if (mode != Mode.OFF) s.lastActive = mode
        refresh(c)
    }

    fun toggle(c: Context) {
        val s = Store(c)
        setMode(c, if (s.mode == Mode.OFF) s.lastActive else Mode.OFF)
    }

    fun isNightNow(s: Store, now: Calendar = Calendar.getInstance()): Boolean {
        val m = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        return if (s.nightStart <= s.nightEnd) m in s.nightStart until s.nightEnd
        else m >= s.nightStart || m < s.nightEnd
    }

    /** Which look is active right now, or null when the app is off. */
    fun current(c: Context): Boolean? {
        val s = Store(c)
        return when (s.mode) {
            Mode.OFF -> null
            Mode.DAY -> false
            Mode.NIGHT -> true
            Mode.AUTO -> isNightNow(s)
        }
    }

    fun refresh(c: Context) {
        val s = Store(c)
        when (val night = current(c)) {
            null -> Tweaks.restore(c)
            else -> Tweaks.apply(c, s.profile(night))
        }
        scheduleNext(c, s)
        updateSurfaces(c)
    }

    private fun scheduleNext(c: Context, s: Store) {
        val am = c.getSystemService(AlarmManager::class.java)
        val pi = PendingIntent.getBroadcast(
            c, 0, Intent(c, AlarmReceiver::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        am.cancel(pi)
        if (s.mode != Mode.AUTO) return
        val now = Calendar.getInstance()
        val next = listOf(s.nightStart, s.nightEnd).map { minutes ->
            (now.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, minutes / 60); set(Calendar.MINUTE, minutes % 60)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                if (timeInMillis <= now.timeInMillis) add(Calendar.DAY_OF_YEAR, 1)
            }.timeInMillis
        }.min()
        // Inexact on purpose: no extra permission, a few minutes of drift is fine for a screen filter.
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next, pi)
    }

    fun updateSurfaces(c: Context) {
        TileService.requestListeningState(c, ComponentName(c, ModeTile::class.java))
        val mgr = AppWidgetManager.getInstance(c)
        val ids = mgr.getAppWidgetIds(ComponentName(c, ModeWidget::class.java))
        if (ids.isNotEmpty()) ModeWidget.update(c, mgr, ids)
    }
}
