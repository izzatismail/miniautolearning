package com.izzatismail.miniautolearning

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.izzatismail.miniautolearning.dashboard.databinding.ActivityMainBinding

/**
 * Dashboard receives real-time updates via Binder callbacks instead of polling.
 * VehicleService pushes speed, temperature, and door status changes through
 * the oneway IVehicleCallback interface. VehicleManager re-dispatches these
 * from the Binder thread to the main thread before reaching this activity.
 *
 * On startup, a single getVehicleStatus() call loads all initial values
 * instead of making five separate IPC calls. After that, callbacks keep
 * the UI current.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var vehicleManager: VehicleManager

    private val callback = object : VehicleManager.VehicleCallback {
        override fun onSpeedChanged(speed: Int) {
            binding.speedText.text = "$speed km/h"
        }

        override fun onTemperatureChanged(temperature: Int) {
            binding.temperatureText.text = "$temperature°C"
        }

        override fun onDoorLockChanged(locked: Boolean) {
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
    }

    override fun onStart() {
        super.onStart()
        // Load initial state via single IPC call (Parcelable VehicleStatus)
        val status = vehicleManager.getVehicleStatus()
        if (status != null) {
            binding.speedText.text = "${status.speed} km/h"
            binding.fuelText.text = "${status.fuelLevel}%"
            binding.gearText.text = status.gear
            binding.doorsText.text = if (status.doorsLocked) "Locked" else "Unlocked"
            binding.temperatureText.text = "${status.temperature}°C"
        }
    }

    override fun onDestroy() {
        vehicleManager.unbind()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "Dashboard"
    }
}