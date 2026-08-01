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
 * Client-side wrapper that hides all Binder details from app code.
 *
 * Why main-thread re-dispatch: Callback methods (onTemperatureChanged etc.)
 * are invoked by the Binder thread in the client's process. Touching a
 * TextView directly from a Binder thread crashes with
 * CalledFromWrongThreadException. VehicleManager re-posts every callback
 * invocation to the main thread via Handler before the listener sees it.
 */
class VehicleManager(private val context: Context) {

    interface VehicleCallback {
        fun onSpeedChanged(speed: Int)
        fun onTemperatureChanged(temperature: Int)
        fun onDoorLockChanged(locked: Boolean)
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
            mainHandler.post { callback?.onTemperatureChanged(newTemperature) }
        }

        override fun onDoorLockChanged(locked: Boolean) {
            mainHandler.post { callback?.onDoorLockChanged(locked) }
        }
    }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d(TAG, "Service connected on thread ${Thread.currentThread().name}")
            vehicleService = IVehicleService.Stub.asInterface(service)
            bound = true
            callback?.let { registerCallback() }
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

    fun setTemperature(value: Int) {
        try {
            vehicleService?.setTemperature(value)
        } catch (e: RemoteException) {
            Log.e(TAG, "Failed to set temperature", e)
        }
    }

    companion object {
        private const val TAG = "VehicleManager-Climate"
    }
}