package com.izzatismail.miniautolearning;

import com.izzatismail.miniautolearning.IVehicleCallback;
import com.izzatismail.miniautolearning.VehicleStatus;

interface IVehicleService {
    int getSpeed();
    int getFuelLevel();
    int getTemperature();
    void setTemperature(int value);
    boolean areDoorsLocked();
    void lockDoors();
    void unlockDoors();
    String getGear();
    void setGear(String gear);

    /**
     * Returns all vehicle state in a single Binder call.
     *
     * Directionality: VehicleStatus is a non-primitive type. When it appears
     * as a return value, AIDL treats it as an out parameter — data flows from
     * the service (callee) back to the client (caller). If VehicleStatus were
     * instead an in/out method parameter, the object would be marshalled in
     * both directions (client → service → client), which is wasteful when
     * only one direction is needed.
     */
    VehicleStatus getVehicleStatus();

    void registerCallback(IVehicleCallback callback);
    void unregisterCallback(IVehicleCallback callback);
}