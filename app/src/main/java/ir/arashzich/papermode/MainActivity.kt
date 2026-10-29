package ir.arashzich.papermode

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var store: Store
    private lateinit var toggle: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)
        store = Store(this)

        toggle = findViewById(R.id.toggle)
        toggle.setOnClickListener {
            if (!Tweaks.hasPermission(this)) { showSetup(); return@setOnClickListener }
            Controller.setOn(this, !store.on)
            render()
        }

        slider(R.id.tone, R.id.tone_label, R.string.tone, store.tone) { store.tone = it }
        slider(R.id.warmth, R.id.warmth_label, R.string.warmth, store.warmth) { store.warmth = it }

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
        render()
        if (Tweaks.hasPermission(this)) Controller.refresh(this)
    }

    private fun showSetup() {
        findViewById<View>(R.id.setup).visibility = View.VISIBLE
    }

    private fun render() {
        toggle.isSelected = store.on
        toggle.setText(if (store.on) R.string.on else R.string.off)
    }

    private fun slider(bar: Int, label: Int, text: Int, value: Int, save: (Int) -> Unit) {
        val seek = findViewById<SeekBar>(bar)
        val view = findViewById<TextView>(label)
        seek.progress = value
        view.text = getString(text, value)
        seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar, v: Int, user: Boolean) { view.text = getString(text, v) }
            override fun onStartTrackingTouch(s: SeekBar) {}
            override fun onStopTrackingTouch(s: SeekBar) {
                save(s.progress)
                if (store.on && Tweaks.hasPermission(this@MainActivity)) Controller.refresh(this@MainActivity)
            }
        })
    }
}
