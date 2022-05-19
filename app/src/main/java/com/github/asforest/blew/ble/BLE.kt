package com.github.asforest.blew.ble

import java.util.UUID


object BLE {
    /**
     * Device Information Service
     */
    val SERVICE_DEVICE_INFORMATION_0x180A = From(0x180A)
    val CHARACTERISTIC_MANUFACTURER_NAME_0x2A29 = From(0x2A29)
    val CHARACTERISTIC_MODEL_NUMBER_0x2A24 = From(0x2A24)
    val CHARACTERISTIC_SERIAL_NUMBER_0x2A25 = From(0x2A25)
    val CHARACTERISTIC_PNP_0x2A50 = From(0x2A50)

    /**
     * Battery Service
     */
    val SERVICE_BATTERY_0x180F = From(0x180F)
    val CHARACTERISTIC_BATTERY_LEVEL_0x2A19 = From(0x2A19)

    /**
     * HID Service
     */
    val SERVICE_HID_0x1812 = From(0x1812)
    val CHARACTERISTIC_HID_INFORMATION_0x2A4A = From(0x2A4A)
    val CHARACTERISTIC_HID_REPORT_MAP_0x2A4B = From(0x2A4B)
    val CHARACTERISTIC_HID_CONTROL_POINT_0x2A4C = From(0x2A4C)
    val CHARACTERISTIC_HID_REPORT_0x2A4D = From(0x2A4D)
    val CHARACTERISTIC_HID_PROTOCOL_MODE_0x2A4E = From(0x2A4E)

    /**
     * Gatt Characteristic Descriptor
     */
    val DESCRIPTOR_REPORT_REFERENCE_0x2908 = From(0x2908)
    val DESCRIPTOR_CLIENT_CHARACTERISTIC_CONFIGURATION_0x2902 = From(0x2902)
    val DESCRIPTOR_CLIENT_CHARACTERISTIC_PRESENTATION_0x2904 = From(0x2904)

    fun From(value: Int): UUID = UUID.fromString("0000" + String.format("%04X", value and 0xffff) + "-0000-1000-8000-00805F9B34FB")

    var REPORT_ID_KEYBOARD: Byte = 0x01
    var REPORT_ID_MEDIA_KEYS: Byte = 0x02
    var REPORT_ID_GAMEPAD: Byte = 0x03

    /**
     * Main items
     */
    fun INPUT(size: Int): Byte = (0x80 or size).toByte()
    fun OUTPUT(size: Int): Byte = (0x90 or size).toByte()
    fun COLLECTION(size: Int): Byte = (0xA0 or size).toByte()
    fun FEATURE(size: Int): Byte = (0xB0 or size).toByte()
    fun END_COLLECTION(size: Int): Byte = (0xC0 or size).toByte()

    /**
     * Global items
     */
    fun USAGE_PAGE(size: Int): Byte = (0x04 or size).toByte()
    fun LOGICAL_MINIMUM(size: Int): Byte = (0x14 or size).toByte()
    fun LOGICAL_MAXIMUM(size: Int): Byte = (0x24 or size).toByte()
    fun PHYSICAL_MINIMUM(size: Int): Byte = (0x34 or size).toByte()
    fun PHYSICAL_MAXIMUM(size: Int): Byte = (0x44 or size).toByte()
    fun UNIT_EXPONENT(size: Int): Byte = (0x54 or size).toByte()
    fun UNIT(size: Int): Byte = (0x64 or size).toByte()
    fun REPORT_SIZE(size: Int): Byte = (0x74 or size).toByte()
    fun REPORT_ID(size: Int): Byte = (0x84 or size).toByte()
    fun REPORT_COUNT(size: Int): Byte = (0x94 or size).toByte()

    /**
     * Local items
     */
    fun USAGE(size: Int): Byte = (0x08 or size).toByte()
    fun USAGE_MINIMUM(size: Int): Byte = (0x18 or size).toByte()
    fun USAGE_MAXIMUM(size: Int): Byte = (0x28 or size).toByte()
    fun LSB(value: Int): Byte = (value and 0xff).toByte()
    fun MSB(value: Int): Byte = (value shr 8 and 0xff).toByte()
}