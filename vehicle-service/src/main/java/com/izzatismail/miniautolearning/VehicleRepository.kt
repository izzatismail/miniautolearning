package com.izzatismail.miniautolearning

import android.util.Log

/**
 * Thread-safe repository for vehicle state.
 *
 * Why synchronized: Multiple Binder pool threads can enter VehicleRepository
 * concurrently when different clients make IPC calls at the same instant.
 * Without synchronized, a lost update could occur — e.g., thread A reads temperature
 * (22), thread B writes temperature (25), thread A writes temperature (23) based on
 * its stale read, silently overwriting B's change. synchronized ensures each
 * read/write is atomic and that writes are visible to subsequent reads.
 */
class VehicleRepository {

    private var speed = 60
    private var fuelLevel = 78
    private var temperature = 22
    private var doorsLocked = true
    private var gear = "DRIVE"

    @Synchronized
    fun getSpeed(): Int {
        Log.d(TAG, "getSpeed() on thread ${Thread.currentThread().name}")
        return speed
    }

    @Synchronized
    fun getFuelLevel(): Int {
        Log.d(TAG, "getFuelLevel() on thread ${Thread.currentThread().name}")
        return fuelLevel
    }

    @Synchronized
    fun getTemperature(): Int {
        Log.d(TAG, "getTemperature() on thread ${Thread.currentThread().name}")
        return temperature
    }

    @Synchronized
    fun setTemperature(value: Int) {
        Log.d(TAG, "setTemperature($value) on thread ${Thread.currentThread().name}")
        require(value in 16..30) { "Temperature must be between 16 and 30°C" }
        temperature = value
    }

    @Synchronized
    fun areDoorsLocked(): Boolean {
        Log.d(TAG, "areDoorsLocked() on thread ${Thread.currentThread().name}")
        return doorsLocked
    }

    @Synchronized
    fun lockDoors() {
        Log.d(TAG, "lockDoors() on thread ${Thread.currentThread().name}")
        doorsLocked = true
    }

    @Synchronized
    fun unlockDoors() {
        Log.d(TAG, "unlockDoors() on thread ${Thread.currentThread().name}")
        doorsLocked = false
    }

    @Synchronized
    fun getGear(): String {
        Log.d(TAG, "getGear() on thread ${Thread.currentThread().name}")
        return gear
    }

    @Synchronized
    fun setGear(value: String) {
        Log.d(TAG, "setGear($value) on thread ${Thread.currentThread().name}")
        gear = value
    }

    companion object {
        private const val TAG = "VehicleRepository"
    }
}