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

    private class Preset(val id: Int, val tone: Int, val warmth: Int)

    private val presets = listOf(
        Preset(R.id.p_kindle, tone = 22, warmth = 0),   // light paper grey, neutral
        Preset(R.id.p_boox, tone = 34, warmth = 0),     // darker grey, more contrast between page and ink
        Preset(R.id.p_night, tone = 38, warmth = 55),   // dim grey with warm light
    )

    private lateinit var store: Store
    private lateinit var tone: SeekBar
    private lateinit var warmth: SeekBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)
        store = Store(this)

        findViewById<View>(R.id.toggle).setOnClickListener {
            if (!Tweaks.hasPermission(this)) { showSetup(); return@setOnClickListener }
            Controller.setOn(this, !store.on)
            render()
        }

        tone = findViewById(R.id.tone)
        warmth = findViewById(R.id.warmth)
        slider(tone, R.id.tone_label, R.string.tone, store.tone) { store.tone = it }
        slider(warmth, R.id.warmth_label, R.string.warmth, store.warmth) { store.warmth = it }

        presets.forEach { p ->
            findViewById<View>(p.id).setOnClickListener {
                store.tone = p.tone; store.warmth = p.warmth
                tone.progress = p.tone; warmth.progress = p.warmth
                changed()
            }
        }

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
        if (Tweaks.hasPermission(this)) Controller.sync(this)
        render()
    }

    private fun showSetup() {
        findViewById<View>(R.id.setup).visibility = View.VISIBLE
    }

    private fun changed() {
        if (store.on && Tweaks.hasPermission(this)) Controller.refresh(this)
        render()
    }

    private fun render() {
        findViewById<View>(R.id.toggle).isSelected = store.on
        findViewById<TextView>(R.id.state).setText(if (store.on) R.string.on else R.string.off)
        findViewById<TextView>(R.id.state_hint).setText(if (store.on) R.string.tap_off else R.string.tap_on)
        presets.forEach { findViewById<View>(it.id).isSelected = it.tone == store.tone && it.warmth == store.warmth }
    }

    private fun slider(seek: SeekBar, label: Int, text: Int, value: Int, save: (Int) -> Unit) {
        val view = findViewById<TextView>(label)
        seek.progress = value
        view.text = getString(text, value)
        seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar, v: Int, user: Boolean) { view.text = getString(text, v) }
            override fun onStartTrackingTouch(s: SeekBar) {}
            override fun onStopTrackingTouch(s: SeekBar) { save(s.progress); changed() }
        })
    }

    private fun bindSchedule() {
        val sw = findViewById<Switch>(R.id.schedule)
        val from = findViewById<TextView>(R.id.t_from)
        val to = findViewById<TextView>(R.id.t_to)
        val times = findViewById<View>(R.id.times)
        fun label(res: Int, m: Int) = "${getString(res)}  %02d:%02d".format(m / 60, m % 60)
        fun render() {
            from.text = label(R.string.from, store.from)
            to.text = label(R.string.to, store.to)
            times.alpha = if (store.schedule) 1f else 0.4f
        }
        fun pick(current: Int, done: (Int) -> Unit) = TimePickerDialog(
            this, { _, h, m -> done(h * 60 + m); render(); if (Tweaks.hasPermission(this)) Controller.sync(this); render() },
            current / 60, current % 60, true,
        ).show()

        sw.isChecked = store.schedule
        sw.setOnCheckedChangeListener { _, on ->
            store.schedule = on
            render()
            if (Tweaks.hasPermission(this)) Controller.sync(this)
            this@MainActivity.render()
        }
        from.setOnClickListener { pick(store.from) { store.from = it } }
        to.setOnClickListener { pick(store.to) { store.to = it } }
        render()
    }
}
