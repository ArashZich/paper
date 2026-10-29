package ir.arashzich.papermode

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Boot, app update, clock/timezone change: re-apply and re-arm the schedule. */
class SystemReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = Controller.refresh(context)
}

/** Fires at the next day/night boundary. */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = Controller.refresh(context)
}
