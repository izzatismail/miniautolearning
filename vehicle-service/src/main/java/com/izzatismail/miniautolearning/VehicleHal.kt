package com.izzatismail.miniautolearning

import android.os.Handler
import android.os.Looper
import android.util.Log

/**
 * Simulates the Vehicle HAL (Hardware Abstraction Layer).
 *
 * In real Android Automotive, the Vehicle HAL communicates with the physical
 * vehicle network (CAN bus) and reports changes up to CarService. Here,
 * VehicleHal generates periodic events to simulate what a real CAN bus would
 * produce: speed changes, fuel level drift, and occasional door toggles.
 *
 * VehicleHal owns the simulation loop and updates VehicleRepository
 * (thread-safely — VehicleRepository methods are synchronized). When state
 * changes, it notifies VehicleService via the HalCallback interface so that
 * callbacks can be broadcast to all registered clients.
 */
class VehicleHal(
    private val repository: VehicleRepository,
    private val callback: HalCallback
) {

    interface HalCallback {
        fun onSpeedChanged(speed: Int)
        fun onTemperatureChanged(temperature: Int)
        fun onDoorLockChanged(locked: Boolean)
    }

    private val handler = Handler(Looper.getMainLooper())
    private var speedDirection = 1

    private val canRunnable = object : Runnable {
        override fun run() {
            simulateCanMessage()
            handler.postDelayed(this, 5000L)
        }
    }

    fun start() {
        Log.d(TAG, "Vehicle HAL started — simulating CAN bus messages")
        handler.post(canRunnable)
    }

    fun stop() {
        Log.d(TAG, "Vehicle HAL stopped")
        handler.removeCallbacks(canRunnable)
    }

    private fun simulateCanMessage() {
        val currentSpeed = repository.getSpeed()
        val nextSpeed = currentSpeed + (5 * speedDirection)
        if (nextSpeed >= 100) speedDirection = -1
        if (nextSpeed <= 30) speedDirection = 1
        repository.setSpeedRaw(nextSpeed)
        Log.d(TAG, "CAN: speed changed to $nextSpeed km/h")
        callback.onSpeedChanged(nextSpeed)
    }

    companion object {
        private const val TAG = "VehicleHal"
    }
}