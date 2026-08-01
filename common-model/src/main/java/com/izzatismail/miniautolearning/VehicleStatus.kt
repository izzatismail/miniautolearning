package com.izzatismail.miniautolearning

import android.os.Parcel
import android.os.Parcelable

/**
 * Combined vehicle state returned in a single Binder call.
 *
 * Using Parcelable (not Serializable) because this object crosses a Binder
 * boundary — Parcelable is the Android-native serialization for IPC and is
 * significantly more efficient than Java Serializable (no reflection, no
 * temporary objects).
 */
data class VehicleStatus(
    val speed: Int,
    val fuelLevel: Int,
    val gear: String,
    val temperature: Int,
    val doorsLocked: Boolean
) : Parcelable {

    constructor(parcel: Parcel) : this(
        speed = parcel.readInt(),
        fuelLevel = parcel.readInt(),
        gear = parcel.readString() ?: "DRIVE",
        temperature = parcel.readInt(),
        doorsLocked = parcel.readByte() != 0.toByte()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(speed)
        parcel.writeInt(fuelLevel)
        parcel.writeString(gear)
        parcel.writeInt(temperature)
        parcel.writeByte(if (doorsLocked) 1 else 0)
    }

    override fun describeContents(): Int = 0

    companion object {
        @JvmField
        val CREATOR = object : Parcelable.Creator<VehicleStatus> {
            override fun createFromParcel(parcel: Parcel): VehicleStatus = VehicleStatus(parcel)
            override fun newArray(size: Int): Array<VehicleStatus?> = arrayOfNulls(size)
        }
    }
}