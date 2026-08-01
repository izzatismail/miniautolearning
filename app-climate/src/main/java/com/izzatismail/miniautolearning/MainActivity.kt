package com.izzatismail.miniautolearning

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.izzatismail.miniautolearning.climate.databinding.ActivityMainBinding

/**
 * Climate app that allows adjusting the vehicle cabin temperature.
 *
 * When the user presses +/- , setTemperature() is called via Binder on the
 * VehicleService. The service updates VehicleRepository and broadcasts the
 * change to all registered clients via the oneway IVehicleCallback interface.
 * Both Climate and Dashboard receive the onTemperatureChanged callback and
 * update their UIs automatically — proving shared state through Binder IPC.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var vehicleManager: VehicleManager

    private val callback = object : VehicleManager.VehicleCallback {
        override fun onSpeedChanged(speed: Int) {
            // Climate doesn't display speed
        }

        override fun onTemperatureChanged(temperature: Int) {
            binding.temperatureText.text = "$temperature°C"
        }

        override fun onDoorLockChanged(locked: Boolean) {
            // Climate doesn't display door status
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        vehicleManager = VehicleManager(this)

        binding.increaseButton.setOnClickListener {
            val current = parseTemperature(binding.temperatureText.text.toString())
            val next = current + 1
            if (next <= 30) {
                Log.d(TAG, "Setting temperature to $next°C")
                vehicleManager.setTemperature(next)
            } else {
                Toast.makeText(this, "Max 30°C", Toast.LENGTH_SHORT).show()
            }
        }

        binding.decreaseButton.setOnClickListener {
            val current = parseTemperature(binding.temperatureText.text.toString())
            val next = current - 1
            if (next >= 16) {
                Log.d(TAG, "Setting temperature to $next°C")
                vehicleManager.setTemperature(next)
            } else {
                Toast.makeText(this, "Min 16°C", Toast.LENGTH_SHORT).show()
            }
        }

        vehicleManager.bind(callback)
    }

    override fun onDestroy() {
        vehicleManager.unbind()
        super.onDestroy()
    }

    private fun parseTemperature(text: String): Int {
        return text.removeSuffix("°C").toIntOrNull() ?: 22
    }

    companion object {
        private const val TAG = "Climate"
    }
}