package itan.com.bluetoothle.kt

import java.util.UUID

object BleConst {

    // Service UUID
    val SERVICE_UUID: UUID = UUID.fromString("0000181c-0000-1000-8000-00805f9b34fb")

    // Characteristic UUID for reading value (Central reads Peripheral)
    val CHARACTERISTIC_READ_UUID: UUID = UUID.fromString("0000fff3-0000-1000-8000-00805f9b34fb")
    // Characteristic UUID for writing value (Peripheral sends data to Central)
    val CHARACTERISTIC_WRITE_UUID: UUID = UUID.fromString("0000fff1-0000-1000-8000-00805f9b34fb")

    val CHARACTERISTIC_NOTIFY_UUID: UUID = UUID.fromString("0000fff2-0000-1000-8000-00805f9b34fb")
    val CHARACTERISTIC_DESCRIPTOR: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb") // DO NOT MODIFY!!
}