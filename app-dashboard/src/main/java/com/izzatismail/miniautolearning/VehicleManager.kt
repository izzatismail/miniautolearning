package com.izzatismail.miniautolearning

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.RemoteException
import android.util.Log

/**
 * Client-side wrapper that hides all Binder details from app code.
 *
 * Apps must never call Binder directly — they talk only to VehicleManager.
 * In Phase 2 when callbacks are introduced, VehicleManager will also be
 * responsible for re-posting callback invocations from the Binder thread
 * to the app's main thread, because touching UI (e.g., updating a TextView)
 * from a Binder thread crashes with CalledFromWrongThreadException.
 */
class VehicleManager(private val context: Context) {

    private var vehicleService: IVehicleService? = null
    private var bound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d(TAG, "Service connected on thread ${Thread.currentThread().name}")
            vehicleService = IVehicleService.Stub.asInterface(service)
            bound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d(TAG, "Service disconnected")
            vehicleService = null
            bound = false
        }
    }

    fun bind() {
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
            context.unbindService(connection)
            bound = false
        }
    }

    fun getSpeed(): Int {
        return vehicleService?.speed ?: -1
    }

    fun getFuelLevel(): Int {
        return vehicleService?.fuelLevel ?: -1
    }

    fun getTemperature(): Int {
        return vehicleService?.temperature ?: -1
    }

    fun areDoorsLocked(): Boolean {
        return vehicleService?.areDoorsLocked() ?: false
    }

    fun getGear(): String {
        return vehicleService?.gear ?: "UNKNOWN"
    }

    companion object {
        private const val TAG = "VehicleManager"
    }
}