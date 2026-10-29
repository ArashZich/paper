package ir.arashzich.papermode

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.service.quicksettings.TileService

/** Single place that makes the saved choice real: apply when on, restore when off. */
object Controller {

    fun setOn(c: Context, on: Boolean) {
        Store(c).on = on
        refresh(c)
    }

    fun toggle(c: Context) = setOn(c, !Store(c).on)

    fun refresh(c: Context) {
        val s = Store(c)
        if (s.on) Tweaks.apply(c, s) else Tweaks.restore(c)
        TileService.requestListeningState(c, ComponentName(c, ModeTile::class.java))
        val mgr = AppWidgetManager.getInstance(c)
        val ids = mgr.getAppWidgetIds(ComponentName(c, ModeWidget::class.java))
        if (ids.isNotEmpty()) ModeWidget.update(c, mgr, ids)
    }
}

/** After boot or an app update the settings may have been reset, so apply the saved choice again. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = Controller.refresh(context)
}
