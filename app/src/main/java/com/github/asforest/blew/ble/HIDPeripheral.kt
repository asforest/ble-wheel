package com.github.asforest.blew.ble

import android.bluetooth.BluetoothAdapter
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
import android.os.Handler
import android.os.ParcelUuid
import android.util.Log
import com.github.asforest.blew.event.Event


abstract class HIDPeripheral(context: Context)
{
    val manufacturer = "manufacturer"
    val deviceName = "device-name"
    val serialNumber = "87654321"

    val handler = Handler(context.mainLooper)
    val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    val adapter: BluetoothAdapter = manager.adapter
    val advertiser: BluetoothLeAdvertiser = setupAdvertiser()
    val gattCallback = GattServerCallback(this)
    val advertiseCallback: AdvertiseCallback = AdvCallback()
    val gattServer: BluetoothGattServer = manager.openGattServer(context, gattCallback)
    var onlineDevices: MutableMap<String, BluetoothDevice> = mutableMapOf()
    var currentDevice: BluetoothDevice? = null
    val hid: BLEHIDDevices = BLEHIDDevices(gattServer, this, currentDevice)

    val onDeviceConnectionStateChangeEvent = Event<Boolean>()
    val onServiceAddEvent = Event<BluetoothGattService>()
    val onCharacteristicReadEvent = Event<CharacteristicReadEvent>()
    val onCharacteristicWriteEvent = Event<CharacteristicWriteEvent>()
    val onDescriptorReadEvent = Event<DescriptorReadEvent>()
    val onDescriptorWriteEvent = Event<DescriptorWriteEvent>()

    init {
        hid.setManufacturer(manufacturer)
        hid.setDeviceName(deviceName)
        hid.setSerialNumber(serialNumber)
    }

    fun setupAdvertiser(): BluetoothLeAdvertiser
    {
        if (!adapter.isEnabled)
            throw UnsupportedOperationException("Bluetooth is disabled.")

        return adapter.bluetoothLeAdvertiser
    }

    fun startAdvertising()
    {
        handler.post {
            // set up advertising setting
            val advertiseSettings = AdvertiseSettings.Builder()
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                .setConnectable(true)
                .setTimeout(0)
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                .build()

            // set up advertising data
            val advertiseData = AdvertiseData.Builder()
                .setIncludeTxPowerLevel(true)
                .setIncludeDeviceName(true)
                .addServiceUuid(ParcelUuid.fromString(BLE.SERVICE_HID_0x1812.toString()))
                .build()

            advertiser.startAdvertising(advertiseSettings, advertiseData, advertiseCallback)
        }
    }

    fun stopAdvertising()
    {
        handler.post {
            try {
                advertiser.stopAdvertising(advertiseCallback)
                for (device in onlineDevices.values)
                    gattServer.cancelConnection(device)
                gattServer.close()
            } catch (ignored: IllegalStateException) {
                // BT Adapter is not turned ON
            }
        }
    }

    class AdvCallback : AdvertiseCallback() {
        override fun onStartFailure(errorCode: Int)
        {
            throw IllegalStateException("广播启动失败: errorCode: $errorCode")
        }
    }

    class GattServerCallback(var ins: HIDPeripheral) : BluetoothGattServerCallback()
    {
        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int)
        {
            Log.d("App", "onConnectionStateChange() device: (${device.name}): $status -> $newState")

//            ins.currentDevice = when(newState) {
//                BluetoothProfile.STATE_CONNECTED -> device
//                BluetoothProfile.STATE_DISCONNECTED -> null
//                else -> null
//            }

            ins.currentDevice = when(newState) {
                BluetoothProfile.STATE_CONNECTED -> if (device.bondState == BluetoothDevice.BOND_BONDED) device else null
                BluetoothProfile.STATE_DISCONNECTED -> null
                else -> null
            }

            ins.onDeviceConnectionStateChangeEvent.invoke(ins.currentDevice != null)
        }

        override fun onServiceAdded(status: Int, service: BluetoothGattService) {
            Log.d("App", "Service Add: ${service.uuid}: $status")
            ins.onServiceAddEvent.invoke(service)
        }

        override fun onCharacteristicReadRequest(device: BluetoothDevice, requestId: Int, offset: Int, characteristic: BluetoothGattCharacteristic)
        {
            Log.d("App", "onCharacteristicReadRequest(): name: (${device.name}), requestId: ($requestId), offset: ($offset), uuid(${characteristic.uuid})")

            ins.onCharacteristicReadEvent.invoke(CharacteristicReadEvent(device, requestId, offset, characteristic))

            ins.gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, characteristic.value)
        }

        override fun onCharacteristicWriteRequest(device: BluetoothDevice, requestId: Int, characteristic: BluetoothGattCharacteristic, preparedWrite: Boolean, responseNeeded: Boolean, offset: Int, value: ByteArray)
        {
            Log.d("App", "onCharacteristicWriteRequest(): name: ${device.name}(${device.address}), requestId: ($requestId), preparedWrite: ($preparedWrite), responseNeeded: ($responseNeeded), offset: ($offset), uuid:(${characteristic.uuid}), value:(${value})")

            ins.onCharacteristicWriteEvent.invoke(CharacteristicWriteEvent(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value))
        }

        override fun onDescriptorReadRequest(device: BluetoothDevice, requestId: Int, offset: Int, descriptor: BluetoothGattDescriptor)
        {
            Log.d("App", "onDescriptorReadRequest() device(${device.name}), requestId($requestId), offset($offset), uuid(${descriptor.uuid})")

            ins.onDescriptorReadEvent.invoke(DescriptorReadEvent(device, requestId, offset, descriptor))

            when(descriptor.uuid)
            {
                BLE.DESCRIPTOR_CLIENT_CHARACTERISTIC_CONFIGURATION_0x2902 -> {
                    val returnValue = if (ins.currentDevice != null && ins.currentDevice == device) {
                        BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    } else {
                        BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
                    }

                    ins.gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, returnValue)
                }

                else -> {
                    ins.gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, descriptor.value)
                }
            }
        }

        override fun onDescriptorWriteRequest(device: BluetoothDevice, requestId: Int, descriptor: BluetoothGattDescriptor, preparedWrite: Boolean, responseNeeded: Boolean, offset: Int, value: ByteArray)
        {
            Log.d("App", "onDescriptorWriteRequest() device(${device.name}), requestId($requestId), offset($offset), uuid(${descriptor.uuid})")

            ins.onDescriptorWriteEvent.invoke(DescriptorWriteEvent(device, requestId, offset, descriptor))
        }
    }

    class CharacteristicWriteEvent(
        val device: BluetoothDevice,
        val requestId: Int,
        val characteristic: BluetoothGattCharacteristic,
        val preparedWrite: Boolean,
        val responseNeeded: Boolean,
        val offset: Int,
        val value: ByteArray
    )

    class DescriptorWriteEvent(
        val device: BluetoothDevice,
        val requestId: Int,
        val offset: Int,
        val descriptor: BluetoothGattDescriptor
    )

    class CharacteristicReadEvent(
        val device: BluetoothDevice,
        val requestId: Int,
        val offset: Int,
        val characteristic: BluetoothGattCharacteristic
    )

    class DescriptorReadEvent(
        val device: BluetoothDevice,
        val requestId: Int,
        val offset: Int,
        val descriptor: BluetoothGattDescriptor
    )
}
