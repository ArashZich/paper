package ir.arashzich.papermode

import android.content.Context

/** The few choices the user has, in one small SharedPreferences file. */
class Store(context: Context) {
    private val p = context.applicationContext.getSharedPreferences("paper", Context.MODE_PRIVATE)

    var on: Boolean
        get() = p.getBoolean("on", false)
        set(v) = p.edit().putBoolean("on", v).apply()

    /** How grey the white gets, 0..100 (0 = pure white, 100 = dark grey). Default is a light paper grey. */
    var tone: Int
        get() = p.getInt("tone", 22)
        set(v) = p.edit().putInt("tone", v).apply()

    /** Optional blue-light filter for night reading, 0 = off. */
    var warmth: Int
        get() = p.getInt("warmth", 0)
        set(v) = p.edit().putInt("warmth", v).apply()

    // Settings values as they were before we touched them (key -> value, or "\u0000" for "was unset").
    fun saved(): Map<String, String> = p.getStringSet("backup", emptySet())!!
        .mapNotNull { e -> e.indexOf('=').takeIf { it > 0 }?.let { e.substring(0, it) to e.substring(it + 1) } }
        .toMap()

    fun saveBackup(m: Map<String, String>) =
        p.edit().putStringSet("backup", m.map { "${it.key}=${it.value}" }.toSet()).apply()
}
