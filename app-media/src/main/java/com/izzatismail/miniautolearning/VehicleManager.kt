package com.izzatismail.miniautolearning

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.RemoteException
import android.util.Log

/**
 * Read-only client wrapper for the Media app.
 * Registers for callbacks with main-thread re-dispatch.
 */
class VehicleManager(private val context: Context) {

    interface VehicleCallback {
        fun onSpeedChanged(speed: Int)
        fun onGearChanged(gear: String)
        fun onFuelChanged(fuel: Int)
    }

    private var vehicleService: IVehicleService? = null
    private var bound = false
    private var callback: VehicleCallback? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private val serviceCallback = object : IVehicleCallback.Stub() {
        override fun onSpeedChanged(newSpeed: Int) {
            mainHandler.post { callback?.onSpeedChanged(newSpeed) }
        }

        override fun onTemperatureChanged(newTemperature: Int) {
            // Media doesn't display temperature
        }

        override fun onDoorLockChanged(locked: Boolean) {
            // Media doesn't display door status
        }
    }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d(TAG, "Service connected on thread ${Thread.currentThread().name}")
            vehicleService = IVehicleService.Stub.asInterface(service)
            bound = true
            registerCallback()
            loadInitialState()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d(TAG, "Service disconnected")
            vehicleService = null
            bound = false
        }
    }

    fun bind(callback: VehicleCallback) {
        this.callback = callback
        val intent = Intent().apply {
            component = ComponentName(
                "com.izzatismail.miniautolearning.vehicleservice",
                "com.izzatismail.miniautolearning.VehicleService"
            )
        }
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    fun unbind() {
        if (bound) {
            unregisterCallback()
            context.unbindService(connection)
            bound = false
        }
    }

    private fun loadInitialState() {
        try {
            val status = vehicleService?.vehicleStatus
            if (status != null) {
                mainHandler.post {
                    callback?.onSpeedChanged(status.speed)
                    callback?.onGearChanged(status.gear)
                    callback?.onFuelChanged(status.fuelLevel)
                }
            }
        } catch (e: RemoteException) {
            Log.e(TAG, "Failed to load initial state", e)
        }
    }

    private fun registerCallback() {
        try {
            vehicleService?.registerCallback(serviceCallback)
        } catch (e: RemoteException) {
            Log.e(TAG, "Failed to register callback", e)
        }
    }

    private fun unregisterCallback() {
        try {
            vehicleService?.unregisterCallback(serviceCallback)
        } catch (e: RemoteException) {
            Log.e(TAG, "Failed to unregister callback", e)
        }
    }

    companion object {
        private const val TAG = "VehicleManager-Media"
    }
}