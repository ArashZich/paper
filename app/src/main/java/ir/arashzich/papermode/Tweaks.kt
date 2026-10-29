package ir.arashzich.papermode

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings

/**
 * Applies a [Profile] through system settings. Needs WRITE_SECURE_SETTINGS (granted once over adb).
 * Every key we change is remembered the first time, and put back by [restore].
 */
object Tweaks {
    private const val UNSET = "\u0000"

    private enum class Table { SECURE, GLOBAL, SYSTEM }

    private class Key(val table: Table, val name: String)

    private val GRAY_ON = Key(Table.SECURE, "accessibility_display_daltonizer_enabled")
    private val GRAY_MODE = Key(Table.SECURE, "accessibility_display_daltonizer")
    private val CONTRAST = Key(Table.SECURE, "high_text_contrast_enabled")
    private val WARM_ON = Key(Table.SECURE, "night_display_activated")
    private val WARM_TEMP = Key(Table.SECURE, "night_display_color_temperature")
    private val DIM_ON = Key(Table.SECURE, "reduce_bright_colors_activated")
    private val DIM_LEVEL = Key(Table.SECURE, "reduce_bright_colors_level")
    private val ANIM = listOf("window_animation_scale", "transition_animation_scale", "animator_duration_scale")
        .map { Key(Table.GLOBAL, it) }
    private val PAPER_ON = Key(Table.SYSTEM, "screen_paper_mode_enabled")

    fun hasPermission(c: Context) =
        c.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED

    fun adbCommand(c: Context) = "adb shell pm grant ${c.packageName} android.permission.WRITE_SECURE_SETTINGS"

    private fun read(c: Context, k: Key): String? = when (k.table) {
        Table.SECURE -> Settings.Secure.getString(c.contentResolver, k.name)
        Table.GLOBAL -> Settings.Global.getString(c.contentResolver, k.name)
        Table.SYSTEM -> Settings.System.getString(c.contentResolver, k.name)
    }

    private fun write(c: Context, k: Key, v: String?) {
        runCatching {
            when (k.table) {
                Table.SECURE -> Settings.Secure.putString(c.contentResolver, k.name, v)
                Table.GLOBAL -> Settings.Global.putString(c.contentResolver, k.name, v)
                Table.SYSTEM -> Settings.System.putString(c.contentResolver, k.name, v)
            }
        }
    }

    fun apply(c: Context, p: Profile) {
        if (!hasPermission(c)) return
        val store = Store(c)
        val backup = store.saved().toMutableMap()
        fun set(k: Key, v: String?) {
            backup.getOrPut(k.name) { read(c, k) ?: UNSET }
            write(c, k, v)
        }

        set(GRAY_ON, if (p.grayscale) "1" else "0")
        if (p.grayscale) set(GRAY_MODE, "0") // 0 = monochromacy

        set(CONTRAST, if (p.highContrast) "1" else "0")

        val anim = if (p.noAnimations) "0" else "1"
        ANIM.forEach { set(it, anim) }

        set(WARM_ON, if (p.warmth > 0) "1" else "0")
        if (p.warmth > 0) set(WARM_TEMP, kelvin(p.warmth).toString())

        set(DIM_ON, if (p.dim > 0) "1" else "0")
        if (p.dim > 0) set(DIM_LEVEL, (p.dim * 100 / 100).coerceIn(0, 100).toString())

        if (p.xiaomiPaper) set(PAPER_ON, "1") else if (backup.containsKey(PAPER_ON.name)) set(PAPER_ON, "0")

        store.saveBackup(backup)
    }

    fun restore(c: Context) {
        if (!hasPermission(c)) return
        val store = Store(c)
        val keys = (listOf(GRAY_ON, GRAY_MODE, CONTRAST, WARM_ON, WARM_TEMP, DIM_ON, DIM_LEVEL, PAPER_ON) + ANIM)
            .associateBy { it.name }
        store.saved().forEach { (name, v) ->
            keys[name]?.let { write(c, it, if (v == UNSET) null else v) }
        }
        store.saveBackup(emptyMap())
    }

    /** 1..100 % -> 4082 K (barely warm) .. 2596 K (very warm), the range Night Light accepts. */
    private fun kelvin(percent: Int) = 4082 - (4082 - 2596) * percent.coerceIn(1, 100) / 100
}
