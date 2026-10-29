package ir.arashzich.papermode

import android.app.PendingIntent
import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

class ModeTile : TileService() {
    override fun onStartListening() = render()

    override fun onClick() {
        if (!Tweaks.hasPermission(this)) {
            startActivityAndCollapse(
                PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE),
            )
            return
        }
        Controller.toggle(this)
        render()
    }

    private fun render() {
        val t = qsTile ?: return
        t.state = if (Store(this).on) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        t.updateTile()
    }
}
