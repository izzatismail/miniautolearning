package com.izzatismail.miniautolearning;

import com.izzatismail.miniautolearning.IVehicleCallback;

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
    void registerCallback(IVehicleCallback callback);
    void unregisterCallback(IVehicleCallback callback);
}