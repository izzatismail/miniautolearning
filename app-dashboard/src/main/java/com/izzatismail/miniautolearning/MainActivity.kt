package com.izzatismail.miniautolearning

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.izzatismail.miniautolearning.dashboard.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var vehicleManager: VehicleManager
    private val handler = Handler(Looper.getMainLooper())
    private val pollRunnable = object : Runnable {
        override fun run() {
            refreshData()
            handler.postDelayed(this, 1000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        vehicleManager = VehicleManager(this)
        vehicleManager.bind()
        handler.post(pollRunnable)
    }

    override fun onDestroy() {
        handler.removeCallbacks(pollRunnable)
        vehicleManager.unbind()
        super.onDestroy()
    }

    private fun refreshData() {
        Log.d(TAG, "Requesting speed...")
        val speed = vehicleManager.getSpeed()
        val fuel = vehicleManager.getFuelLevel()
        val temperature = vehicleManager.getTemperature()
        val doorsLocked = vehicleManager.areDoorsLocked()
        val gear = vehicleManager.getGear()

        binding.speedText.text = "$speed km/h"
        binding.fuelText.text = "$fuel%"
        binding.temperatureText.text = "$temperature°C"
        binding.doorsText.text = if (doorsLocked) "Locked" else "Unlocked"
        binding.gearText.text = gear
    }

    companion object {
        private const val TAG = "Dashboard"
    }
}