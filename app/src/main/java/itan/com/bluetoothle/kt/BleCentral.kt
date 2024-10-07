package itan.com.bluetoothle.kt

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.content.Context
import android.util.Log
import itan.com.bluetoothle.kt.BleConst.CHARACTERISTIC_DESCRIPTOR
import itan.com.bluetoothle.kt.BleConst.CHARACTERISTIC_NOTIFY_UUID
import itan.com.bluetoothle.kt.BleConst.CHARACTERISTIC_READ_UUID
import itan.com.bluetoothle.kt.BleConst.CHARACTERISTIC_WRITE_UUID
import itan.com.bluetoothle.kt.BleConst.SERVICE_UUID

class BleCentral(private val context: Context) {
    private var bluetoothGatt: BluetoothGatt? = null

    private var onDataReceived: ((String) -> Unit)? = null
    private var onDataNotified: ((String) -> Unit)? = null

    // 페리페럴에 연결
    fun connectToPeripheral(device: BluetoothDevice) {
        bluetoothGatt = device.connectGatt(context, false, gattCallback)
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                Log.i("BleCentral", "Connected to peripheral, discovering services...")
                gatt.discoverServices()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i("BleCentral", "Services discovered")
                gatt.services.forEach { service ->
                    // 서비스 내 모든 Characteristic 탐색
                    service.characteristics.forEach { characteristic ->
                        when {
//                            characteristic.isReadable() -> {
////                                readCharacteristic(characteristic)
//                                readCharacteristic = characteristic
//                            }
//                            characteristic.isWritable() -> {
////                                writeCharacteristic(characteristic, "Hello".toByteArray())
//                                writeCharacteristic = characteristic
//                            }
                            characteristic.isNotifiable() -> enableNotifications(characteristic)
                        }
                    }
                }
            }
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i("BleCentral", "Read data from ${characteristic.uuid}: ${String(characteristic.value)}")
                onDataReceived?.invoke(String(characteristic.value))
            }
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i("BleCentral", "Data written to ${characteristic.uuid}")
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            Log.i("BleCentral", "Notification from ${characteristic.uuid}: ${String(characteristic.value)}")
            onDataNotified?.invoke(String(characteristic.value))
        }
    }

    private fun enableNotifications(characteristic: BluetoothGattCharacteristic) {
        val service = bluetoothGatt?.getService(SERVICE_UUID)
        val notifyCharacteristic = service?.getCharacteristic(CHARACTERISTIC_NOTIFY_UUID)
        bluetoothGatt?.setCharacteristicNotification(notifyCharacteristic, true)

        val descriptor = notifyCharacteristic?.getDescriptor(CHARACTERISTIC_DESCRIPTOR)
        descriptor?.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        bluetoothGatt?.writeDescriptor(descriptor)
    }

    fun setupCharacteristic(readCallback: (String) -> Unit, notifyCallback: (String) -> Unit,) {
        onDataReceived = readCallback
        onDataNotified = notifyCallback
    }

    fun readCharacteristic() {
        val service = bluetoothGatt?.getService(SERVICE_UUID)
        val readCharacteristic = service?.getCharacteristic(CHARACTERISTIC_READ_UUID)
        bluetoothGatt?.readCharacteristic(readCharacteristic)
    }

    fun writeCharacteristic(data: ByteArray) {
        val service = bluetoothGatt?.getService(SERVICE_UUID)
        val writeCharacteristic = service?.getCharacteristic(CHARACTERISTIC_WRITE_UUID)
        writeCharacteristic?.value = data
        bluetoothGatt?.writeCharacteristic(writeCharacteristic)
    }
}

// 확장 함수로 Characteristic 속성 확인
fun BluetoothGattCharacteristic.isReadable(): Boolean {
    return properties and BluetoothGattCharacteristic.PROPERTY_READ != 0
}

fun BluetoothGattCharacteristic.isWritable(): Boolean {
    return properties and (BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0
}

fun BluetoothGattCharacteristic.isNotifiable(): Boolean {
    return properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0
}