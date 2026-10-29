package ir.arashzich.papermode

import android.content.Context

enum class Mode { OFF, DAY, NIGHT, AUTO }

/** One look: what gets applied to the whole system while it is active. */
data class Profile(
    val grayscale: Boolean,
    val warmth: Int,        // 0 = off, 100 = warmest the system allows
    val dim: Int,           // 0 = off, 100 = darkest "reduce bright colors" allows
    val noAnimations: Boolean,
    val highContrast: Boolean,
    val xiaomiPaper: Boolean,
) {
    companion object {
        val DAY = Profile(grayscale = true, warmth = 30, dim = 0, noAnimations = true, highContrast = true, xiaomiPaper = false)
        val NIGHT = Profile(grayscale = true, warmth = 85, dim = 35, noAnimations = true, highContrast = false, xiaomiPaper = false)
    }
}

/** All user choices, kept in one small SharedPreferences file. */
class Store(context: Context) {
    private val p = context.applicationContext.getSharedPreferences("paper", Context.MODE_PRIVATE)

    var mode: Mode
        get() = runCatching { Mode.valueOf(p.getString("mode", null)!!) }.getOrDefault(Mode.OFF)
        set(v) = p.edit().putString("mode", v.name).apply()

    /** Last mode that was not OFF, so the tile/widget can switch back to it. */
    var lastActive: Mode
        get() = runCatching { Mode.valueOf(p.getString("last", null)!!) }.getOrDefault(Mode.AUTO)
        set(v) = p.edit().putString("last", v.name).apply()

    /** Night starts/ends, minutes after midnight. */
    var nightStart: Int
        get() = p.getInt("nightStart", 20 * 60)
        set(v) = p.edit().putInt("nightStart", v).apply()
    var nightEnd: Int
        get() = p.getInt("nightEnd", 6 * 60)
        set(v) = p.edit().putInt("nightEnd", v).apply()

    fun profile(night: Boolean): Profile {
        val k = if (night) "n_" else "d_"
        val d = if (night) Profile.NIGHT else Profile.DAY
        return Profile(
            grayscale = p.getBoolean(k + "gray", d.grayscale),
            warmth = p.getInt(k + "warm", d.warmth),
            dim = p.getInt(k + "dim", d.dim),
            noAnimations = p.getBoolean(k + "anim", d.noAnimations),
            highContrast = p.getBoolean(k + "contrast", d.highContrast),
            xiaomiPaper = p.getBoolean(k + "paper", d.xiaomiPaper),
        )
    }

    fun save(night: Boolean, v: Profile) {
        val k = if (night) "n_" else "d_"
        p.edit()
            .putBoolean(k + "gray", v.grayscale).putInt(k + "warm", v.warmth).putInt(k + "dim", v.dim)
            .putBoolean(k + "anim", v.noAnimations).putBoolean(k + "contrast", v.highContrast)
            .putBoolean(k + "paper", v.xiaomiPaper).apply()
    }

    // Settings values as they were before we touched them (key -> value or "\u0000" for "was unset").
    fun saved(): Map<String, String> = p.getStringSet("backup", emptySet())!!
        .mapNotNull { e -> e.indexOf('=').takeIf { it > 0 }?.let { e.substring(0, it) to e.substring(it + 1) } }
        .toMap()

    fun saveBackup(m: Map<String, String>) =
        p.edit().putStringSet("backup", m.map { "${it.key}=${it.value}" }.toSet()).apply()
}
