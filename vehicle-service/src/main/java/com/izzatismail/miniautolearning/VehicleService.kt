package com.izzatismail.miniautolearning

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.RemoteCallbackList
import android.util.Log

/**
 * Bound service that owns the vehicle state and processes IPC requests from
 * multiple client apps. Runs in the ":vehicle" process to enforce process
 * separation — even if a client crashes, the service remains unaffected.
 */
class VehicleService : Service() {

    private val repository = VehicleRepository()

    // Why RemoteCallbackList instead of a plain ArrayList<IVehicleCallback>:
    // RemoteCallbackList internally uses linkToDeath on each registered callback's
    // Binder. If a client process dies unexpectedly, RemoteCallbackList automatically
    // removes its callback entry. A plain ArrayList silently retains dead references,
    // causing the service to try notifying a zombie client — which would throw a
    // DeadObjectException during the broadcast loop.
    private val callbacks = RemoteCallbackList<IVehicleCallback>()

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
            repository.setGear(gear)
        }

        override fun registerCallback(callback: IVehicleCallback) {
            Log.d(TAG, "Received registerCallback() on thread ${Thread.currentThread().name}")
            callbacks.register(callback)
            callbacks.finishBroadcast()
        }

        override fun unregisterCallback(callback: IVehicleCallback) {
            Log.d(TAG, "Received unregisterCallback() on thread ${Thread.currentThread().name}")
            callbacks.unregister(callback)
        }
    }

    override fun onBind(intent: Intent?): IBinder {
        Log.d(TAG, "onBind() on thread ${Thread.currentThread().name}")
        return binder
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy()")
        callbacks.kill()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "VehicleService"
    }
}