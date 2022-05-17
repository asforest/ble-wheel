package com.github.asforest.blew.ble

import android.bluetooth.*
import android.util.Log
import com.github.asforest.blew.event.Event


class GattServerCallback(var ins: HidPeripheral) : BluetoothGattServerCallback()
{
    val Tag = "APP"

    val onCharacteristicWriteEvent = Event<CharacteristicWriteEvent>()
    val onDescriptorWriteEvent = Event<DescriptorWriteEvent>()
    val onServiceAddEvent = Event<BluetoothGattService>()

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

    override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int)
    {
        Log.d(Tag, "onConnectionStateChange() device: (${device.name}): $status -> $newState")

        ins.currentDevice = when(newState) {
            BluetoothProfile.STATE_CONNECTED -> device
            BluetoothProfile.STATE_DISCONNECTED -> null
            else -> null
        }
    }

    override fun onServiceAdded(status: Int, service: BluetoothGattService) {
        Log.d("App", "Service Add: ${service.uuid}: $status")
        onServiceAddEvent.invoke(service)
    }

    override fun onCharacteristicReadRequest(device: BluetoothDevice, requestId: Int, offset: Int, characteristic: BluetoothGattCharacteristic)
    {
        Log.d(Tag, "onCharacteristicReadRequest(): name: (${device.name}), requestId: ($requestId), offset: ($offset), uuid(${characteristic.uuid})")

        ins.gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, characteristic.value)
    }

    override fun onCharacteristicWriteRequest(device: BluetoothDevice, requestId: Int, characteristic: BluetoothGattCharacteristic, preparedWrite: Boolean, responseNeeded: Boolean, offset: Int, value: ByteArray)
    {
        Log.d(Tag, "onCharacteristicWriteRequest(): name: ${device.name}(${device.address}), requestId: ($requestId), preparedWrite: ($preparedWrite), responseNeeded: ($responseNeeded), offset: ($offset), uuid:(${characteristic.uuid}), value:(${value})")

        onCharacteristicWriteEvent.invoke(CharacteristicWriteEvent(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value))
    }

    override fun onDescriptorReadRequest(device: BluetoothDevice, requestId: Int, offset: Int, descriptor: BluetoothGattDescriptor)
    {
        Log.d(Tag, "onDescriptorReadRequest() device(${device.name}), requestId($requestId), offset($offset), uuid(${descriptor.uuid})")

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
        Log.d(Tag, "onDescriptorWriteRequest() device(${device.name}), requestId($requestId), offset($offset), uuid(${descriptor.uuid})")

        onDescriptorWriteEvent.invoke(DescriptorWriteEvent(device, requestId, offset, descriptor))
    }
}