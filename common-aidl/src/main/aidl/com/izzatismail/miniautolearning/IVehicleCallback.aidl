package com.izzatismail.miniautolearning;

/**
 * Why oneway: Callback notifications are sent to all registered clients in a loop
 * (fan-out). Without oneway, a slow or unresponsive client would stall the entire
 * notification loop, blocking VehicleService from processing new requests. oneway
 * makes each callback fire-and-forget, so the service is never at the mercy of a
 * client's responsiveness. The trade-off is that the callback invocation order is
 * not strictly guaranteed and delivery is best-effort.
 */
oneway interface IVehicleCallback {
    void onSpeedChanged(int newSpeed);
    void onTemperatureChanged(int newTemperature);
    void onDoorLockChanged(boolean locked);
}