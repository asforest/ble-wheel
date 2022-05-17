package com.github.asforest.blew.ble

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor

/**
 * See also:
 * https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.descriptor.gatt.characteristic_presentation_format.xml
 */
class BLE2904 : BluetoothGattDescriptor(
    BLE.DESCRIPTOR_CLIENT_CHARACTERISTIC_PRESENTATION_0x2904,
    BluetoothGattCharacteristic.PERMISSION_READ
) {
    var format: Byte = 0
    var exponent: Byte = 0

    /**
     * Set the units for this value.  It should be one of the encoded values defined here:
     * https://www.bluetooth.com/specifications/assigned-numbers/units
     */
    var unit: Short = 0
    var namespace: Byte = 1 // 1 = Bluetooth SIG Assigned Numbers
    var description: Short = 0

    init {
        applyModification()
    }

    fun applyModification()
    {
        value = byteArrayOf(
            format,
            exponent,
            (unit.toInt() shr 8).toByte(), unit.toByte(),
            namespace,
            (description.toInt() shr 8).toByte(), description.toByte(),
        )
    }

    companion object{
        val FORMAT_BOOLEAN: Byte   = 1
        val FORMAT_UINT2: Byte     = 2
        val FORMAT_UINT4: Byte     = 3
        val FORMAT_UINT8: Byte     = 4
        val FORMAT_UINT12: Byte    = 5
        val FORMAT_UINT16: Byte    = 6
        val FORMAT_UINT24: Byte    = 7
        val FORMAT_UINT32: Byte    = 8
        val FORMAT_UINT48: Byte    = 9
        val FORMAT_UINT64: Byte    = 10
        val FORMAT_UINT128: Byte   = 11
        val FORMAT_SINT8: Byte     = 12
        val FORMAT_SINT12: Byte    = 13
        val FORMAT_SINT16: Byte    = 14
        val FORMAT_SINT24: Byte    = 15
        val FORMAT_SINT32: Byte    = 16
        val FORMAT_SINT48: Byte    = 17
        val FORMAT_SINT64: Byte    = 18
        val FORMAT_SINT128: Byte   = 19
        val FORMAT_FLOAT32: Byte   = 20
        val FORMAT_FLOAT64: Byte   = 21
        val FORMAT_SFLOAT16: Byte  = 22
        val FORMAT_SFLOAT32: Byte  = 23
        val FORMAT_IEEE20601: Byte = 24
        val FORMAT_UTF8: Byte      = 25
        val FORMAT_UTF16: Byte     = 26
        val FORMAT_OPAQUE: Byte    = 27
    }
}