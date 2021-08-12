package com.example.w2.hid

import android.bluetooth.*
import android.util.Log
import com.example.w2.hid.UUIDs.CLEAN_KEY_REPORT
import java.lang.String
import java.util.*


class GattServerCallback : BluetoothGattServerCallback
{
    var ins: HidPeripheral
    var TAG = "TAG"
    var def_mtu = 23
    var mtu = def_mtu

    constructor(ins: HidPeripheral)
    {
        this.ins = ins
        TAG = ins.TAG
    }

    override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int)
    {
        super.onConnectionStateChange(device, status, newState)
        Log.d(TAG, "onConnectionStateChange status: device: (${device.name}): $status -> $newState")

        if(newState == BluetoothProfile.STATE_CONNECTED) {
            mtu = def_mtu
            ins.currentCentral = device
        } else if(newState == BluetoothProfile.STATE_DISCONNECTED) {
            ins.currentCentral = null
        }
    }

    override fun onServiceAdded(status: Int, service: BluetoothGattService)
    {
        super.onServiceAdded(status, service)
        if (service.uuid.equals(UUIDs.SERVICE_BLE_HID_0x1812))
            Log.d(TAG, if (status === BluetoothGatt.GATT_SUCCESS) "Added HID service." else "Failed to add HID service.")
    }

    override fun onCharacteristicReadRequest(device: BluetoothDevice, requestId: Int, offset: Int, characteristic: BluetoothGattCharacteristic)
    {
        super.onCharacteristicReadRequest(device, requestId, offset, characteristic)
        Log.d(TAG, "onCharacteristicReadRequest(): name: (${device.name}), requestId: ($requestId), offset: ($offset), uuid(${characteristic.uuid})")

        val gatt = ins.gattServer!!
        val SUCCESS = BluetoothGatt.GATT_SUCCESS

        when(characteristic.uuid)
        {
            UUIDs.CHARACTERISTIC_HID_INFORMATION_0x2A4A -> {
                Log.d(TAG, "read hid information");
                var value = byteArrayOf(
                    0x11, 0x01,     // HID spec v1.11
                    0x00,           // country code 00
                    0x03            // flags: normally_connectable: 0:off 1:on, remote_wake- 0:off 1:on
                )
                gatt.sendResponse(device, requestId, SUCCESS, offset, value)
            }

            UUIDs.CHARACTERISTIC_HID_REPORT_MAP_0x2A4B -> {
                Log.d(TAG, "read hid report map");
                gatt.sendResponse(device, requestId, SUCCESS, offset, UUIDs.HID_RM)
            }

            UUIDs.CHARACTERISTIC_HID_REPORT_0x2A4D -> {
                Log.d(TAG, "read hid report: ->");

                when (characteristic) {
                    ins.inputReportCharacteristic -> {
                        Log.d(TAG, "    read from input report")
                        gatt.sendResponse(device, requestId, SUCCESS, offset, CLEAN_KEY_REPORT)
                    }

                    ins.outputReportCharacteristic -> {
                        Log.d(TAG, "    read from out report");
                        var kbLock = byteArrayOf(0)
                        gatt.sendResponse(device, requestId, SUCCESS, offset, kbLock)
                    }

                    else -> {
                        Log.d(TAG, "    read some else report");
                    }
                }
            }

            UUIDs.CHARACTERISTIC_HID_PROTOCOL_MODE_0x2A4E -> {
                Log.d(TAG, "read hid protocol mode");
                gatt.sendResponse(device, requestId, SUCCESS, offset, byteArrayOf(0x01))
            }

            else -> {
                Log.e(TAG, "Received read request from unknown characteristic.");
            }
        }

    }

    override fun onCharacteristicWriteRequest(device: BluetoothDevice, requestId: Int, characteristic: BluetoothGattCharacteristic, preparedWrite: Boolean, responseNeeded: Boolean, offset: Int, value: ByteArray)
    {
        super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value)
        Log.d(TAG, "onCharacteristicWriteRequest(): name: ${device.name}(${device.address}), requestId: ($requestId), preparedWrite: ($preparedWrite), responseNeeded: ($responseNeeded), offset: ($offset), uuid:(${characteristic.uuid}), value:(${value})")

        var status = BluetoothGatt.GATT_SUCCESS

        when(characteristic.uuid)
        {
            UUIDs.CHARACTERISTIC_HID_CONTROL_POINT_0x2A4C -> {
                Log.d(TAG, "have write Request to control point")
            }

            UUIDs.CHARACTERISTIC_HID_REPORT_0x2A4D -> {
                val lock = value[0]
                Log.d(TAG, "Received write request to keyboard lock status: lock($lock)")
//                mKeyboard.setKeyboardLock(lock)
            }

            else -> {
                Log.e(TAG, String.format("Received write request from unknown characteristic. (%s)", characteristic.uuid.toString()))
                status = BluetoothGatt.GATT_FAILURE
            }
        }
    }

    override fun onDescriptorReadRequest(device: BluetoothDevice, requestId: Int, offset: Int, descriptor: BluetoothGattDescriptor)
    {
        super.onDescriptorReadRequest(device, requestId, offset, descriptor)
        Log.d(TAG, "onDescriptorReadRequest() device(${device.name}), requestId($requestId), offset($offset), uuid(${descriptor.uuid})")

        val PT_READ = BluetoothGattCharacteristic.PROPERTY_READ
        val PT_WRITE = BluetoothGattCharacteristic.PROPERTY_WRITE
        val PT_WRITE_NR = BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE
        val PT_NOTIFY = BluetoothGattCharacteristic.PROPERTY_NOTIFY

        when(descriptor.uuid)
        {
            UUIDs.DESCRIPTOR_CLIENT_CHARACTERISTIC_CONFIGURATION_0x2902 -> {
                Log.d(TAG, "Config descriptor read")

                val returnValue = if (ins.currentCentral != null && ins.currentCentral == device) {
                    Log.d(TAG, "notify register success");
                    BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                } else {
                    Log.d(TAG, "notify register fail");
                    BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
                }

                ins.gattServer!!.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, returnValue)
            }

            UUIDs.DESCRIPTOR_REPORT_REFERENCE_0x2908 -> {
//                Log.d(TAG, "read report reference descriptor, this should not happen")
//                Log.d(TAG, "read report reference descriptor")

                when (descriptor.characteristic.properties) {
                    PT_READ or PT_NOTIFY -> {
                        // Input Report
                        ins.gattServer!!.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, byteArrayOf(UUIDs.KEYBOARD_ID, 1))
                    }
                    PT_READ or PT_WRITE or PT_WRITE_NR -> {
                        // Output Report
                        ins.gattServer!!.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, byteArrayOf(UUIDs.KEYBOARD_ID, 2))
                    }
                    PT_READ or PT_WRITE -> {
                        // Feature Report
                        ins.gattServer!!.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, byteArrayOf(UUIDs.KEYBOARD_ID, 3))
                    }
                    else -> {
                        ins.gattServer!!.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, 0, byteArrayOf())
                    }
                }
            }

            else -> {
                Log.w(TAG, "Unknown descriptor read request");
                ins.gattServer!!.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, 0, null);
            }
        }
    }

    // dis
    override fun onDescriptorWriteRequest(device: BluetoothDevice, requestId: Int, descriptor: BluetoothGattDescriptor, preparedWrite: Boolean, responseNeeded: Boolean, offset: Int, value: ByteArray)
    {
        super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value)
        Log.d(TAG, "onDescriptorWriteRequest() device(${device.name}), requestId($requestId), offset($offset), uuid(${descriptor.uuid})")

//        var status = BluetoothGatt.GATT_SUCCESS
//        val uuid = descriptor.uuid
//
//        if (uuid == UUIDs.DESCRIPTOR_CLIENT_CHARACTERISTIC_CONFIGURATION_0x2902) {
//            if (Arrays.equals(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE, value)) {
//                Log.d(TAG, "Subscribe device to notifications: $device")
//                ins.currentCentral = device
//            } else if (Arrays.equals(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE, value)) {
//                Log.d(TAG, "Unsubscribe device from notifications: $device")
//                ins.currentCentral = null
//            }
//        } else {
//            Log.d(TAG, "Unknown descriptor write request.")
//            status = BluetoothGatt.GATT_FAILURE
//        }
    }

//    override fun onNotificationSent(device: BluetoothDevice, status: Int)
//    {
//        super.onNotificationSent(device, status)
//        Log.d(TAG, "onNotificationSent() device(${device.name}),  status($status)")
//    }

    override fun onMtuChanged(device: BluetoothDevice, mtu: Int)
    {
        super.onMtuChanged(device, mtu)
        this.mtu = mtu
    }

}