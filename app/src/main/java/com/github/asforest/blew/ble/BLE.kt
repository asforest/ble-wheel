package com.github.asforest.blew.ble

import java.util.*

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
    val SERVICE_BLE_HID_0x1812 = From(0x1812)
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

    val CLEAN_KEY_REPORT = byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0)

    fun From(value: Int): UUID = UUID.fromString("0000" + String.format("%04X", value and 0xffff) + "-0000-1000-8000-00805F9B34FB")

    var REPORT_ID_KEYBOARD: Byte = 0x01
    var REPORT_ID_MEDIA_KEYS: Byte = 0x02

    val HID_REPORT_MAP_KEYBOARD = byteArrayOf(
        USAGE_PAGE(1),        0x01,  // USAGE_PAGE (Generic Desktop Ctrls)
        USAGE(1),             0x06,  // USAGE (Keyboard)
        COLLECTION(1),        0x01,  // COLLECTION (Application)
        // ------------------------------------------------- Keyboard
        REPORT_ID(1),         REPORT_ID_KEYBOARD,  //   REPORT_ID (1)
        USAGE_PAGE(1),        0x07,                //   USAGE_PAGE (Kbrd/Keypad)
        USAGE_MINIMUM(1),     0xE0.toByte(),       //   USAGE_MINIMUM (0xE0)
        USAGE_MAXIMUM(1),     0xE7.toByte(),       //   USAGE_MAXIMUM (0xE7)
        LOGICAL_MINIMUM(1),   0x00,                //   LOGICAL_MINIMUM (0)
        LOGICAL_MAXIMUM(1),   0x01,                //   Logical Maximum (1)
        REPORT_SIZE(1),       0x01,                //   REPORT_SIZE (1)
        REPORT_COUNT(1),      0x08,                //   REPORT_COUNT (8)
        INPUT(1),             0x02,                //   INPUT (Data,Var,Abs,No Wrap,Linear,Preferred State,No Null Position)
        REPORT_COUNT(1),      0x01,                //   REPORT_COUNT (1) ; 1 byte (Reserved)
        REPORT_SIZE(1),       0x08,                //   REPORT_SIZE (8)
        INPUT(1),             0x01,                //   INPUT (Const,Array,Abs,No Wrap,Linear,Preferred State,No Null Position)
        REPORT_COUNT(1),      0x05,                //   REPORT_COUNT (5) ; 5 bits (Num lock, Caps lock, Scroll lock, Compose, Kana)
        REPORT_SIZE(1),       0x01,                //   REPORT_SIZE (1)
        USAGE_PAGE(1),        0x08,                //   USAGE_PAGE (LEDs)
        USAGE_MINIMUM(1),     0x01,                //   USAGE_MINIMUM (0x01) ; Num Lock
        USAGE_MAXIMUM(1),     0x05,                //   USAGE_MAXIMUM (0x05) ; Kana
        OUTPUT(1),            0x02,                //   OUTPUT (Data,Var,Abs,No Wrap,Linear,Preferred State,No Null Position,Non-volatile)
        REPORT_COUNT(1),      0x01,                //   REPORT_COUNT (1) ; 3 bits (Padding)
        REPORT_SIZE(1),       0x03,                //   REPORT_SIZE (3)
        OUTPUT(1),            0x01,                //   OUTPUT (Const,Array,Abs,No Wrap,Linear,Preferred State,No Null Position,Non-volatile)
        REPORT_COUNT(1),      0x06,                //   REPORT_COUNT (6) ; 6 bytes (Keys)
        REPORT_SIZE(1),       0x08,                //   REPORT_SIZE(8)
        LOGICAL_MINIMUM(1),   0x00,                //   LOGICAL_MINIMUM(0)
        LOGICAL_MAXIMUM(1),   0x65,                //   LOGICAL_MAXIMUM(0x65) ; 101 keys
        USAGE_PAGE(1),        0x07,                //   USAGE_PAGE (Kbrd/Keypad)
        USAGE_MINIMUM(1),     0x00,                //   USAGE_MINIMUM (0)
        USAGE_MAXIMUM(1),     0x65,                //   USAGE_MAXIMUM (0x65)
        INPUT(1),             0x00,                //   INPUT (Data,Array,Abs,No Wrap,Linear,Preferred State,No Null Position)
        END_COLLECTION(0)
    )

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