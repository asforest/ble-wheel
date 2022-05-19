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
    var format: UByte = 0u
    var exponent: Byte = 0

    /**
     * Set the units for this value.  It should be one of the encoded values defined here:
     * https://www.bluetooth.com/specifications/assigned-numbers/units
     */
    var unit: UShort = 0u
    var namespace: UByte = 1u // 1 = Bluetooth SIG Assigned Numbers
    var description: UShort = 0u

    init {
        applyModification()
    }

    fun applyModification()
    {
        value = byteArrayOf(
            format.toByte(),
            exponent,
            (unit.toInt() shr 8).toByte(), unit.toByte(),
            namespace.toByte(),
            (description.toInt() shr 8).toByte(), description.toByte(),
        )
    }

    companion object{
        val FORMAT_BOOLEAN: UByte   = 1u
        val FORMAT_UINT2: UByte     = 2u
        val FORMAT_UINT4: UByte     = 3u
        val FORMAT_UINT8: UByte     = 4u
        val FORMAT_UINT12: UByte    = 5u
        val FORMAT_UINT16: UByte    = 6u
        val FORMAT_UINT24: UByte    = 7u
        val FORMAT_UINT32: UByte    = 8u
        val FORMAT_UINT48: UByte    = 9u
        val FORMAT_UINT64: UByte    = 10u
        val FORMAT_UINT128: UByte   = 11u
        val FORMAT_SINT8: UByte     = 12u
        val FORMAT_SINT12: UByte    = 13u
        val FORMAT_SINT16: UByte    = 14u
        val FORMAT_SINT24: UByte    = 15u
        val FORMAT_SINT32: UByte    = 16u
        val FORMAT_SINT48: UByte    = 17u
        val FORMAT_SINT64: UByte    = 18u
        val FORMAT_SINT128: UByte   = 19u
        val FORMAT_FLOAT32: UByte   = 20u
        val FORMAT_FLOAT64: UByte   = 21u
        val FORMAT_SFLOAT16: UByte  = 22u
        val FORMAT_SFLOAT32: UByte  = 23u
        val FORMAT_IEEE20601: UByte = 24u
        val FORMAT_UTF8: UByte      = 25u
        val FORMAT_UTF16: UByte     = 26u
        val FORMAT_OPAQUE: UByte    = 27u
    }
}