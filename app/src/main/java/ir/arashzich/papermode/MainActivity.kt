package ir.arashzich.papermode

import android.app.Activity
import android.app.TimePickerDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var store: Store
    private val modeButtons = LinkedHashMap<Mode, TextView>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)
        store = Store(this)

        modeButtons[Mode.OFF] = findViewById(R.id.m_off)
        modeButtons[Mode.DAY] = findViewById(R.id.m_day)
        modeButtons[Mode.NIGHT] = findViewById(R.id.m_night)
        modeButtons[Mode.AUTO] = findViewById(R.id.m_auto)
        modeButtons.forEach { (mode, v) ->
            v.setOnClickListener {
                if (!Tweaks.hasPermission(this)) { showSetup(); return@setOnClickListener }
                Controller.setMode(this, mode)
                renderMode()
            }
        }

        bindProfile(findViewById(R.id.day), night = false, title = R.string.day_look)
        bindProfile(findViewById(R.id.night), night = true, title = R.string.night_look)
        bindSchedule()

        findViewById<TextView>(R.id.command).text = Tweaks.adbCommand(this)
        findViewById<View>(R.id.copy).setOnClickListener {
            getSystemService(ClipboardManager::class.java)
                .setPrimaryClip(ClipData.newPlainText("adb", Tweaks.adbCommand(this)))
            Toast.makeText(this, R.string.copied, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        findViewById<View>(R.id.setup).visibility = if (Tweaks.hasPermission(this)) View.GONE else View.VISIBLE
        renderMode()
        // Re-apply in case the system or another app changed the settings while we were away.
        if (Tweaks.hasPermission(this)) Controller.refresh(this)
    }

    private fun showSetup() {
        findViewById<View>(R.id.setup).visibility = View.VISIBLE
    }

    private fun renderMode() {
        val cur = store.mode
        modeButtons.forEach { (m, v) -> v.isSelected = m == cur }
    }

    private fun bindProfile(root: View, night: Boolean, title: Int) {
        root.findViewById<TextView>(R.id.title).setText(title)
        val gray = root.findViewById<Switch>(R.id.gray)
        val contrast = root.findViewById<Switch>(R.id.contrast)
        val anim = root.findViewById<Switch>(R.id.anim)
        val paper = root.findViewById<Switch>(R.id.paper)
        val warm = root.findViewById<SeekBar>(R.id.warm)
        val dim = root.findViewById<SeekBar>(R.id.dim)
        val warmLabel = root.findViewById<TextView>(R.id.warm_label)
        val dimLabel = root.findViewById<TextView>(R.id.dim_label)

        val p = store.profile(night)
        gray.isChecked = p.grayscale
        contrast.isChecked = p.highContrast
        anim.isChecked = p.noAnimations
        paper.isChecked = p.xiaomiPaper
        warm.progress = p.warmth
        dim.progress = p.dim
        warmLabel.text = getString(R.string.warmth, p.warmth)
        dimLabel.text = getString(R.string.dim, p.dim)

        fun commit() {
            store.save(
                night,
                Profile(gray.isChecked, warm.progress, dim.progress, anim.isChecked, contrast.isChecked, paper.isChecked),
            )
            if (Tweaks.hasPermission(this)) Controller.refresh(this)
        }

        listOf(gray, contrast, anim, paper).forEach { it.setOnCheckedChangeListener { _, _ -> commit() } }
        fun seek(bar: SeekBar, label: TextView, res: Int) = bar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar, v: Int, user: Boolean) { label.text = getString(res, v) }
            override fun onStartTrackingTouch(s: SeekBar) {}
            override fun onStopTrackingTouch(s: SeekBar) = commit()
        })
        seek(warm, warmLabel, R.string.warmth)
        seek(dim, dimLabel, R.string.dim)
    }

    private fun bindSchedule() {
        val from = findViewById<TextView>(R.id.t_from)
        val to = findViewById<TextView>(R.id.t_to)
        fun label(res: Int, min: Int) = "${getString(res)}\n%02d:%02d".format(min / 60, min % 60)
        fun render() {
            from.text = label(R.string.night_from, store.nightStart)
            to.text = label(R.string.night_to, store.nightEnd)
        }
        fun pick(current: Int, onDone: (Int) -> Unit) =
            TimePickerDialog(this, { _, h, m -> onDone(h * 60 + m); render(); Controller.refresh(this) }, current / 60, current % 60, true).show()
        from.setOnClickListener { pick(store.nightStart) { store.nightStart = it } }
        to.setOnClickListener { pick(store.nightEnd) { store.nightEnd = it } }
        render()
    }
}
