package ir.arashzich.papermode

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

/** Home-screen widget: four buttons (Off / Day / Night / Auto), the active one is highlighted. */
class ModeWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, mgr: AppWidgetManager, ids: IntArray) = update(context, mgr, ids)

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_SET) {
            val mode = runCatching { Mode.valueOf(intent.getStringExtra(EXTRA_MODE)!!) }.getOrNull() ?: return
            if (Tweaks.hasPermission(context)) Controller.setMode(context, mode)
            else context.startActivity(Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    companion object {
        private const val ACTION_SET = "ir.arashzich.papermode.SET_MODE"
        private const val EXTRA_MODE = "mode"

        private val buttons = mapOf(
            Mode.OFF to R.id.w_off, Mode.DAY to R.id.w_day, Mode.NIGHT to R.id.w_night, Mode.AUTO to R.id.w_auto,
        )

        fun update(c: Context, mgr: AppWidgetManager, ids: IntArray) {
            val current = Store(c).mode
            val views = RemoteViews(c.packageName, R.layout.widget).apply {
                buttons.forEach { (mode, id) ->
                    setBoolean(id, "setSelected", mode == current)
                    val i = Intent(c, ModeWidget::class.java).setAction(ACTION_SET).putExtra(EXTRA_MODE, mode.name)
                    setOnClickPendingIntent(
                        id,
                        PendingIntent.getBroadcast(c, mode.ordinal, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE),
                    )
                }
            }
            mgr.updateAppWidget(ids, views)
        }
    }
}
