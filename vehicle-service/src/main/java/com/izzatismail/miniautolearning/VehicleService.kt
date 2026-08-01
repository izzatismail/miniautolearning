package com.izzatismail.miniautolearning

import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.IBinder
import android.os.RemoteCallbackList
import android.util.Log

/**
 * Bound service that owns the vehicle state and processes IPC requests from
 * multiple client apps. Runs in the ":vehicle" process to enforce process
 * separation — even if a client crashes, the service remains unaffected.
 *
 * Vehicle behaviour is split across layers mirroring Android Automotive:
 *   VehicleService  →  VehicleHal  →  VehicleRepository
 *
 * VehicleService handles IPC and permission enforcement.
 * VehicleHal simulates CAN bus events (periodic speed, fuel, door changes).
 * VehicleRepository provides thread-safe (synchronized) state access.
 *
 * Sensitive operations (lockDoors, unlockDoors, setGear) are gated by a
 * real Android permission check. Unlike a simulated if-check, this throws
 * a SecurityException that the calling process cannot ignore — the same
 * mechanism Android Automotive uses to prevent untrusted apps from
 * remotely unlocking doors.
 */
class VehicleService : Service() {

    private val repository = VehicleRepository()

    private val callbacks = RemoteCallbackList<IVehicleCallback>()

    private val halCallback = object : VehicleHal.HalCallback {
        override fun onSpeedChanged(speed: Int) {
            Log.d(TAG, "HAL callback: onSpeedChanged($speed), broadcasting to callbacks")
            val count = callbacks.beginBroadcast()
            for (i in 0 until count) {
                callbacks.getBroadcastItem(i).onSpeedChanged(speed)
            }
            callbacks.finishBroadcast()
        }

        override fun onTemperatureChanged(temperature: Int) {
            Log.d(TAG, "HAL callback: onTemperatureChanged($temperature), broadcasting to callbacks")
            val count = callbacks.beginBroadcast()
            for (i in 0 until count) {
                callbacks.getBroadcastItem(i).onTemperatureChanged(temperature)
            }
            callbacks.finishBroadcast()
        }

        override fun onDoorLockChanged(locked: Boolean) {
            Log.d(TAG, "HAL callback: onDoorLockChanged($locked), broadcasting to callbacks")
            val count = callbacks.beginBroadcast()
            for (i in 0 until count) {
                callbacks.getBroadcastItem(i).onDoorLockChanged(locked)
            }
            callbacks.finishBroadcast()
        }
    }

    private val hal = VehicleHal(repository, halCallback)

    // Store context reference for permission checking inside the Stub
    // (cannot use this@VehicleService inside anonymous object — triggers
    // email address mangling in some editors)
    private var serviceContext: android.content.Context? = null

    private val binder = object : IVehicleService.Stub() {

        override fun getSpeed(): Int {
            Log.d(TAG, "Received getSpeed() on thread ${Thread.currentThread().name}")
            val result = repository.getSpeed()
            Log.d(TAG, "Returning $result km/h")
            return result
        }

        override fun getFuelLevel(): Int {
            Log.d(TAG, "Received getFuelLevel() on thread ${Thread.currentThread().name}")
            val result = repository.getFuelLevel()
            Log.d(TAG, "Returning $result%")
            return result
        }

        override fun getTemperature(): Int {
            Log.d(TAG, "Received getTemperature() on thread ${Thread.currentThread().name}")
            val result = repository.getTemperature()
            Log.d(TAG, "Returning $result°C")
            return result
        }

        override fun setTemperature(value: Int) {
            Log.d(TAG, "Received setTemperature($value) on thread ${Thread.currentThread().name}")
            repository.setTemperature(value)
            Log.d(TAG, "Temperature updated, notifying callbacks (oneway)")
            val count = callbacks.beginBroadcast()
            for (i in 0 until count) {
                callbacks.getBroadcastItem(i).onTemperatureChanged(value)
            }
            callbacks.finishBroadcast()
        }

        override fun areDoorsLocked(): Boolean {
            Log.d(TAG, "Received areDoorsLocked() on thread ${Thread.currentThread().name}")
            val result = repository.areDoorsLocked()
            Log.d(TAG, "Returning locked=$result")
            return result
        }

        override fun lockDoors() {
            Log.d(TAG, "Received lockDoors() on thread ${Thread.currentThread().name}")
            checkControlPermission()
            repository.lockDoors()
            Log.d(TAG, "Doors locked, notifying callbacks (oneway)")
            val count = callbacks.beginBroadcast()
            for (i in 0 until count) {
                callbacks.getBroadcastItem(i).onDoorLockChanged(true)
            }
            callbacks.finishBroadcast()
        }

        override fun unlockDoors() {
            Log.d(TAG, "Received unlockDoors() on thread ${Thread.currentThread().name}")
            checkControlPermission()
            repository.unlockDoors()
            Log.d(TAG, "Doors unlocked, notifying callbacks (oneway)")
            val count = callbacks.beginBroadcast()
            for (i in 0 until count) {
                callbacks.getBroadcastItem(i).onDoorLockChanged(false)
            }
            callbacks.finishBroadcast()
        }

        override fun getGear(): String {
            Log.d(TAG, "Received getGear() on thread ${Thread.currentThread().name}")
            val result = repository.getGear()
            Log.d(TAG, "Returning $result")
            return result
        }

        override fun setGear(gear: String) {
            Log.d(TAG, "Received setGear($gear) on thread ${Thread.currentThread().name}")
            checkControlPermission()
            repository.setGear(gear)
        }

        override fun getVehicleStatus(): VehicleStatus {
            Log.d(TAG, "Received getVehicleStatus() on thread ${Thread.currentThread().name}")
            val result = repository.getVehicleStatus()
            Log.d(TAG, "Returning $result")
            return result
        }

        override fun registerCallback(callback: IVehicleCallback) {
            Log.d(TAG, "Received registerCallback() on thread ${Thread.currentThread().name}")
            callbacks.register(callback)
        }

        override fun unregisterCallback(callback: IVehicleCallback) {
            Log.d(TAG, "Received unregisterCallback() on thread ${Thread.currentThread().name}")
            callbacks.unregister(callback)
        }

        private fun checkControlPermission() {
            val ctx = serviceContext ?: return
            // Why checkCallingOrSelfPermission instead of a simulated if-check:
            // This throws a real SecurityException that propagates across the
            // Binder boundary to the calling process. An if-check returning
            // false would let the caller silently ignore the rejection.
            // This is the same mechanism Android Automotive uses to prevent
            // untrusted apps from remotely unlocking doors or shifting gears.
            if (ctx.checkCallingOrSelfPermission(VehicleService.VEHICLE_CONTROL_PERMISSION)
                != PackageManager.PERMISSION_GRANTED
            ) {
                throw SecurityException("Permission denied: ${VehicleService.VEHICLE_CONTROL_PERMISSION}")
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        serviceContext = this
        Log.d(TAG, "onCreate() — starting Vehicle HAL")
        hal.start()
    }

    override fun onBind(intent: Intent?): IBinder {
        Log.d(TAG, "onBind() on thread ${Thread.currentThread().name}")
        return binder
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy()")
        hal.stop()
        callbacks.kill()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "VehicleService"
        private const val VEHICLE_CONTROL_PERMISSION = "com.example.mini.permission.CONTROL_VEHICLE"
    }
}