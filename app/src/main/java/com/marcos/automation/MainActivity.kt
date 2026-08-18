package com.marcos.automation

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(48, 96, 48, 48)

        val title = TextView(this)
        title.text = "MARCOS Automation"
        title.textSize = 22f
        layout.addView(title)

        val status = TextView(this)

        status.text =
            if (MarcosAccessibilityService.instance != null) {
                "Status: Accessibility service is ACTIVE\nLocal command server running on port 8482."
            } else {
                "Status: Accessibility service is NOT enabled.\n\nTap the button below, then find \"MARCOS Automation\" in the list and turn it on."
            }

        status.setPadding(0, 32, 0, 32)
        layout.addView(status)

        val enableButton = Button(this)
        enableButton.text = "Open Accessibility Settings"

        enableButton.setOnClickListener {
            startActivity(
                Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            )
        }

        layout.addView(enableButton)

        val info = TextView(this)
        info.setPadding(0, 32, 0, 0)

        info.text =
            "Once enabled, MARCOS (in your browser or the MARCOS app) can send it commands at http://localhost:8482 on this device."

        layout.addView(info)

        setContentView(layout)
    }

    override fun onResume() {
        super.onResume()
        recreate()
    }
}
