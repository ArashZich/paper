package ir.arashzich.papermode

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

/** Home-screen widget: one button that switches paper mode on and off. */
class ModeWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, mgr: AppWidgetManager, ids: IntArray) = update(context, mgr, ids)

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action != ACTION_TOGGLE) return
        if (Tweaks.hasPermission(context)) Controller.toggle(context)
        else context.startActivity(Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    companion object {
        private const val ACTION_TOGGLE = "ir.arashzich.papermode.TOGGLE"

        fun update(c: Context, mgr: AppWidgetManager, ids: IntArray) {
            val views = RemoteViews(c.packageName, R.layout.widget).apply {
                setBoolean(R.id.w_toggle, "setSelected", Store(c).on)
                setTextViewText(R.id.w_toggle, c.getString(if (Store(c).on) R.string.on else R.string.off))
                val i = Intent(c, ModeWidget::class.java).setAction(ACTION_TOGGLE)
                setOnClickPendingIntent(
                    R.id.w_toggle,
                    PendingIntent.getBroadcast(c, 0, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE),
                )
            }
            mgr.updateAppWidget(ids, views)
        }
    }
}
