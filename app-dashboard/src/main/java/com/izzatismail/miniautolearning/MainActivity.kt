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
 * Initial state is loaded automatically by VehicleManager once the service
 * connects (via a single getVehicleStatus() Parcelable call).
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

    override fun onDestroy() {
        vehicleManager.unbind()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "Dashboard"
    }
}