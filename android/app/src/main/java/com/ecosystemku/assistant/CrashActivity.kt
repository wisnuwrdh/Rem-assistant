package com.ecosystemku.assistant

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity

class CrashActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val stacktrace = intent.getStringExtra(EXTRA_STACKTRACE)
            ?: "Tidak ada detail error yang terekam."

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#121212"))
            setPadding(40, 80, 40, 48)
        }

        val title = TextView(this).apply {
            text = "Aplikasi Mengalami Error"
            setTextColor(Color.parseColor("#FF6B6B"))
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
        }

        val subtitle = TextView(this).apply {
            text = "Salin log di bawah ini dan kirimkan untuk dianalisis:"
            setTextColor(Color.parseColor("#B0B0B0"))
            textSize = 13f
            setPadding(0, 8, 0, 24)
        }

        val buttonRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.START
            setPadding(0, 0, 0, 24)
        }

        val copyBtn = Button(this).apply {
            text = "Salin Error"
            setOnClickListener {
                val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("Crash Log", stacktrace))
                Toast.makeText(this@CrashActivity, "Error disalin ke clipboard", Toast.LENGTH_SHORT).show()
            }
        }

        val restartBtn = Button(this).apply {
            text = "Buka Ulang"
            setOnClickListener {
                val launchIntent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
                if (launchIntent != null) {
                    startActivity(launchIntent)
                }
                finish()
            }
        }

        buttonRow.addView(copyBtn)
        buttonRow.addView(restartBtn)

        val scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
            setBackgroundColor(Color.parseColor("#1E1E1E"))
            setPadding(24, 24, 24, 24)
        }

        val logText = TextView(this).apply {
            text = stacktrace
            setTextColor(Color.parseColor("#E0E0E0"))
            textSize = 11.5f
            typeface = Typeface.MONOSPACE
            setTextIsSelectable(true)
        }

        scrollView.addView(logText)
        root.addView(title)
        root.addView(subtitle)
        root.addView(buttonRow)
        root.addView(scrollView)

        setContentView(root)
    }

    companion object {
        const val EXTRA_STACKTRACE = "extra_stacktrace"
    }
}
