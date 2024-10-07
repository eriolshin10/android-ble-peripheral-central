package itan.com.bluetoothle.kt

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import itan.com.bluetoothle.kt.BleConst.CHARACTERISTIC_DESCRIPTOR
import itan.com.bluetoothle.kt.BleConst.CHARACTERISTIC_NOTIFY_UUID
import itan.com.bluetoothle.kt.BleConst.CHARACTERISTIC_READ_UUID
import itan.com.bluetoothle.kt.BleConst.CHARACTERISTIC_WRITE_UUID
import itan.com.bluetoothle.kt.BleConst.SERVICE_UUID

class BlePeripheral(private val context: Context) {

    private val TAG = "BlePeripheral"

    private var bluetoothGattServer: BluetoothGattServer? = null
    private var advertiser: BluetoothLeAdvertiser? = null
    private var connectedDevice: BluetoothDevice? = null

    private var onDataReceived: ((String) -> Unit)? = null

    init {
        initialize()
    }

    private fun initialize() {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter
        if (bluetoothAdapter.isEnabled) {
            advertiser = bluetoothAdapter.bluetoothLeAdvertiser
            startAdvertising()
            setupGattServer(bluetoothManager)
        } else {
            Log.e(TAG, "Bluetooth not enabled or available.")
        }
    }

    private fun startAdvertising() {
        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setConnectable(true)
            .build()

        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(true)
            .addServiceUuid(ParcelUuid(SERVICE_UUID))
            .build()

        advertiser?.startAdvertising(settings, data, object : AdvertiseCallback() {
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
                Log.i(TAG, "Advertising started successfully.")
            }

            override fun onStartFailure(errorCode: Int) {
                Log.e(TAG, "Advertising failed with error code: $errorCode")
            }
        })
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun setupGattServer(bluetoothManager: BluetoothManager) {
        bluetoothGattServer = bluetoothManager.openGattServer(context, object : BluetoothGattServerCallback() {
            override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.i(TAG, "Device connected: ${device.address}")
                    connectedDevice = device
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.i(TAG, "Device disconnected: ${device.address}")
                }
            }

            override fun onNotificationSent(device: BluetoothDevice?, status: Int) {
                super.onNotificationSent(device, status)
                Log.i(TAG, "device: $device, statue: $status")
            }

            override fun onCharacteristicReadRequest(device: BluetoothDevice, requestId: Int, offset: Int, characteristic: BluetoothGattCharacteristic) {
                if (characteristic.uuid == CHARACTERISTIC_READ_UUID) {
                    Log.i(TAG, "Read request received from ${device.address}")
                    // Here we send a response with the characteristic value
                    bluetoothGattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, characteristic.value)
                }
            }

            override fun onCharacteristicWriteRequest(device: BluetoothDevice, requestId: Int, characteristic: BluetoothGattCharacteristic, preparedWrite: Boolean, responseNeeded: Boolean, offset: Int, value: ByteArray) {
                if (characteristic.uuid == CHARACTERISTIC_WRITE_UUID) {
                    Log.i(TAG, "Write request received from ${device.address}")
                    // Handle the value written by the central device
                    characteristic.value = value
                    Log.i(TAG, "Received data: ${String(value)}, onDataReceived: ${onDataReceived}")
                    onDataReceived?.invoke(String(value))
                    if (responseNeeded) {
                        bluetoothGattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, null)
                    }
                }
            }

            override fun onDescriptorReadRequest(
                device: BluetoothDevice?,
                requestId: Int,
                offset: Int,
                descriptor: BluetoothGattDescriptor?
            ) {
                super.onDescriptorReadRequest(device, requestId, offset, descriptor)
                Log.d(TAG, "Device tried to read descriptor: ${descriptor?.uuid}")

                bluetoothGattServer?.sendResponse(
                    device,
                    requestId,
                    BluetoothGatt.GATT_SUCCESS,
                    offset,
                    "".toByteArray()
                )
            }

            override fun onDescriptorWriteRequest(
                device: BluetoothDevice?,
                requestId: Int,
                descriptor: BluetoothGattDescriptor?,
                preparedWrite: Boolean,
                responseNeeded: Boolean,
                offset: Int,
                value: ByteArray?
            ) {
                super.onDescriptorWriteRequest(
                    device,
                    requestId,
                    descriptor,
                    preparedWrite,
                    responseNeeded,
                    offset,
                    value
                )
                Log.d(TAG, "device: $device deviceName: ${device?.name} responseNeeded : $responseNeeded, bytes : ${value?.toHexString()}")

                if (responseNeeded) {
                    bluetoothGattServer?.sendResponse(
                        device,
                        requestId,
                        BluetoothGatt.GATT_SUCCESS,
                        offset,
                        "".toByteArray()
                    )
                }
            }
        })

        val service = BluetoothGattService(SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY)

        val readCharacteristic = BluetoothGattCharacteristic(
            CHARACTERISTIC_READ_UUID,
            BluetoothGattCharacteristic.PROPERTY_READ,
            BluetoothGattCharacteristic.PERMISSION_READ
        )
        service.addCharacteristic(readCharacteristic)

        val writeCharacteristic = BluetoothGattCharacteristic(
            CHARACTERISTIC_WRITE_UUID,
            BluetoothGattCharacteristic.PROPERTY_WRITE,
            BluetoothGattCharacteristic.PERMISSION_WRITE
        )
        service.addCharacteristic(writeCharacteristic)

        val notifyCharacteristic = BluetoothGattCharacteristic(
            CHARACTERISTIC_NOTIFY_UUID,
            BluetoothGattCharacteristic.PROPERTY_NOTIFY or BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
            BluetoothGattCharacteristic.PERMISSION_WRITE or BluetoothGattCharacteristic.PERMISSION_READ
        ).apply {
            addDescriptor(
                BluetoothGattDescriptor(
                    CHARACTERISTIC_DESCRIPTOR,
                    BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE
                ).apply {
                    value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                }
            )
            writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
        }
        service.addCharacteristic(notifyCharacteristic)

        bluetoothGattServer?.addService(service)
    }

    fun readCentralDevice(data: ByteArray) {
        val readCharacteristic = bluetoothGattServer?.getService(SERVICE_UUID)?.getCharacteristic(CHARACTERISTIC_READ_UUID)
        readCharacteristic?.setValue(data)
    }

    fun writeCentralDevice(callback: (String) -> Unit) {
        onDataReceived = callback
    }

    fun notifyCentralDevice(data: ByteArray) {
        val notifyCharacteristic = bluetoothGattServer?.getService(SERVICE_UUID)?.getCharacteristic(CHARACTERISTIC_NOTIFY_UUID)
        val indicate = (notifyCharacteristic?.properties?.and(BluetoothGattCharacteristic.PROPERTY_INDICATE)
            ?: false) == BluetoothGattCharacteristic.PROPERTY_INDICATE
        notifyCharacteristic?.value = data
        bluetoothGattServer?.notifyCharacteristicChanged(connectedDevice, notifyCharacteristic, indicate)
    }
}