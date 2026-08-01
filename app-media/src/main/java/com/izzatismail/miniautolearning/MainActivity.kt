package com.izzatismail.miniautolearning

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.izzatismail.miniautolearning.media.databinding.ActivityMainBinding

/**
 * Read-only Media app that displays vehicle speed, gear, and fuel level.
 * Receives speed updates via Binder callbacks; polls for gear and fuel
 * since those don't change autonomously (no CAN simulation yet).
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var vehicleManager: VehicleManager

    private val callback = object : VehicleManager.VehicleCallback {
        override fun onSpeedChanged(speed: Int) {
            binding.speedText.text = "Speed: $speed km/h"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Log.d(TAG, "Starting Media — binding to vehicle service")
        vehicleManager = VehicleManager(this)
        vehicleManager.bind(callback)
    }

    override fun onDestroy() {
        vehicleManager.unbind()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "Media"
    }
}