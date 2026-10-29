package ir.arashzich.papermode

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

class ModeTile : TileService() {
    override fun onStartListening() = render()

    override fun onClick() {
        if (!Tweaks.hasPermission(this)) {
            startActivityAndCollapse(
                android.app.PendingIntent.getActivity(
                    this, 0, android.content.Intent(this, MainActivity::class.java),
                    android.app.PendingIntent.FLAG_IMMUTABLE,
                ),
            )
            return
        }
        Controller.toggle(this)
        render()
    }

    private fun render() {
        val t = qsTile ?: return
        val mode = Store(this).mode
        t.state = if (mode == Mode.OFF) Tile.STATE_INACTIVE else Tile.STATE_ACTIVE
        t.subtitle = getString(modeLabel(mode))
        t.updateTile()
    }
}

fun modeLabel(m: Mode) = when (m) {
    Mode.OFF -> R.string.mode_off
    Mode.DAY -> R.string.mode_day
    Mode.NIGHT -> R.string.mode_night
    Mode.AUTO -> R.string.mode_auto
}
