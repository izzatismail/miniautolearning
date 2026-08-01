package com.izzatismail.miniautolearning

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.izzatismail.miniautolearning.dashboard.databinding.ActivityMainBinding

/**
 * Dashboard displays vehicle state via Binder callbacks with a polling
 * fallback. VehicleManager attempts callback-driven updates first; if
 * callbacks are slow or unreliable, the one-shot poll at startup and
 * the periodic refresh ensure the UI stays current.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var vehicleManager: VehicleManager
    private val handler = Handler(Looper.getMainLooper())

    private val callback = object : VehicleManager.VehicleCallback {
        override fun onSpeedChanged(speed: Int) {
            Log.d(TAG, "Callback: speed=$speed")
            binding.speedText.text = "$speed km/h"
        }

        override fun onTemperatureChanged(temperature: Int) {
            Log.d(TAG, "Callback: temperature=$temperature")
            binding.temperatureText.text = "$temperature°C"
        }

        override fun onDoorLockChanged(locked: Boolean) {
            Log.d(TAG, "Callback: doors=${if (locked) "Locked" else "Unlocked"}")
            binding.doorsText.text = if (locked) "Locked" else "Unlocked"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Log.d(TAG, "Starting Dashboard — binding to vehicle service")
        vehicleManager = VehicleManager(this)
        vehicleManager.bind(callback)

        // Fallback: poll every 5s in case callbacks are slow
        handler.post(object : Runnable {
            override fun run() {
                val status = vehicleManager.getVehicleStatus()
                if (status != null) {
                    binding.speedText.text = "${status.speed} km/h"
                    binding.fuelText.text = "${status.fuelLevel}%"
                    binding.gearText.text = status.gear
                    binding.doorsText.text = if (status.doorsLocked) "Locked" else "Unlocked"
                    binding.temperatureText.text = "${status.temperature}°C"
                }
                handler.postDelayed(this, 5000L)
            }
        })
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        vehicleManager.unbind()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "Dashboard"
    }
}