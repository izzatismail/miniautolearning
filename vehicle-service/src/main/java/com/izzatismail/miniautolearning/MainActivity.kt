package com.izzatismail.miniautolearning

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * Minimal launcher activity for the Vehicle Service module.
 *
 * This activity exists only so that Android Studio deploys the APK when
 * the user presses Run. The actual work happens in VehicleService (bound
 * service running in the :vehicle process). This activity immediately
 * starts the service and displays a status message — no polling, no
 * Binder interaction.
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val textView = TextView(this).apply {
            text = "Vehicle Service running"
            textSize = 18f
            setPadding(24, 24, 24, 24)
        }
        setContentView(textView)

        startService(Intent(this, VehicleService::class.java))
        finish()
    }
}