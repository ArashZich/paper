package ir.arashzich.papermode

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings

/**
 * Makes the screen look like an e-reader through system display settings.
 * Needs WRITE_SECURE_SETTINGS (granted once over adb). The first value of every key we touch is
 * remembered and put back by [restore].
 */
object Tweaks {
    private const val UNSET = "\u0000"

    private class Key(val global: Boolean, val name: String)

    private val GRAY_ON = Key(false, "accessibility_display_daltonizer_enabled")
    private val GRAY_MODE = Key(false, "accessibility_display_daltonizer")
    private val CONTRAST = Key(false, "high_text_contrast_enabled")
    private val LIGHT_UI = Key(false, "ui_night_mode")
    private val DIM_ON = Key(false, "reduce_bright_colors_activated")
    private val DIM_LEVEL = Key(false, "reduce_bright_colors_level")
    private val WARM_ON = Key(false, "night_display_activated")
    private val WARM_TEMP = Key(false, "night_display_color_temperature")
    private val ANIM = listOf("window_animation_scale", "transition_animation_scale", "animator_duration_scale")
        .map { Key(true, it) }
    private val ALL = listOf(GRAY_ON, GRAY_MODE, CONTRAST, LIGHT_UI, DIM_ON, DIM_LEVEL, WARM_ON, WARM_TEMP) + ANIM

    fun hasPermission(c: Context) =
        c.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED

    fun adbCommand(c: Context) = "adb shell pm grant ${c.packageName} android.permission.WRITE_SECURE_SETTINGS"

    // Some hidden keys can be written but not read by apps (SecurityException); treat those as "was unset".
    private fun read(c: Context, k: Key): String? = runCatching {
        if (k.global) Settings.Global.getString(c.contentResolver, k.name)
        else Settings.Secure.getString(c.contentResolver, k.name)
    }.getOrNull()

    private fun write(c: Context, k: Key, v: String?) {
        runCatching {
            if (k.global) Settings.Global.putString(c.contentResolver, k.name, v)
            else Settings.Secure.putString(c.contentResolver, k.name, v)
        }
    }

    fun apply(c: Context, s: Store) {
        if (!hasPermission(c)) return
        val backup = s.saved().toMutableMap()
        fun set(k: Key, v: String) {
            backup.getOrPut(k.name) { read(c, k) ?: UNSET }
            write(c, k, v)
        }

        set(GRAY_ON, "1"); set(GRAY_MODE, "0")      // 0 = monochromacy: no colors at all
        set(CONTRAST, "1")                          // black, bold text
        set(LIGHT_UI, "1")                          // paper is light: never the dark theme
        ANIM.forEach { set(it, "0") }               // a page appears, it does not slide

        // White becomes paper-grey; black stays black.
        if (s.tone > 0) { set(DIM_ON, "1"); set(DIM_LEVEL, s.tone.toString()) } else set(DIM_ON, "0")
        if (s.warmth > 0) { set(WARM_ON, "1"); set(WARM_TEMP, kelvin(s.warmth).toString()) } else set(WARM_ON, "0")

        s.saveBackup(backup)
    }

    fun restore(c: Context) {
        if (!hasPermission(c)) return
        val s = Store(c)
        val byName = ALL.associateBy { it.name }
        s.saved().forEach { (name, v) -> byName[name]?.let { write(c, it, if (v == UNSET) null else v) } }
        s.saveBackup(emptyMap())
    }

    /** 1..100 % -> 4082 K (barely warm) .. 2596 K (very warm), the range Night Light accepts. */
    private fun kelvin(percent: Int) = 4082 - (4082 - 2596) * percent.coerceIn(1, 100) / 100
}
