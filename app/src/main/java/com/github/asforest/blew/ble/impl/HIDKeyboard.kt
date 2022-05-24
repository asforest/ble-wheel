package com.github.asforest.blew.ble.impl

import android.content.Context
import android.util.Log
import com.github.asforest.blew.ble.BLE
import com.github.asforest.blew.ble.HIDPeripheral

//class HIDKeyboard(context: Context) : HIDPeripheral(context)
//{
//    val inputReport = hid.inputReport(BLE.REPORT_ID_KEYBOARD)
//    val outputReport = hid.outputReport(BLE.REPORT_ID_KEYBOARD)
//
//    init {
//        hid.setManufacturer("Asf")
//        hid.pnp(0x02, 0x05ac, (0x820a).toShort(), 0x0210)
//        hid.hidInfo(0x00, 0x01)
//        hid.reportMap(HID_REPORT_MAP_KEYBOARD)
//        hid.setBatteryLevel(98)
//
//        hid.startServices()
//    }
//
//    fun sendMsg(msg :String)
//    {
//        if (currentDevice == null)
//        {
//            Log.i("App", "no connection found")
//            return
//        }
//
//        inputReport.value = byteArrayOf(0, 0, 0x05, 0, 0, 0, 0, 0)
//        gattServer.notifyCharacteristicChanged(currentDevice, inputReport, false)
//
//        Thread.sleep(20)
//
//        inputReport.value = CLEAN_KEY_REPORT
//        gattServer.notifyCharacteristicChanged(currentDevice, inputReport, false)
//
//        Thread.sleep(20)
//
//        hid.setBatteryLevel(50)
//    }
//
//    companion object {
//        val CLEAN_KEY_REPORT = byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0)
//
//        val HID_REPORT_MAP_KEYBOARD = byteArrayOf(
//            BLE.USAGE_PAGE(1),        0x01,                    // USAGE_PAGE (Generic Desktop Ctrls)
//            BLE.USAGE(1),             0x06,                    // USAGE (Keyboard)
//            BLE.COLLECTION(1),        0x01,                    // COLLECTION (Application)
//            // ------------------------------------------------- Keyboard
//            BLE.REPORT_ID(1),         BLE.REPORT_ID_KEYBOARD,  //   REPORT_ID (1)
//            BLE.USAGE_PAGE(1),        0x07,                    //   USAGE_PAGE (Kbrd/Keypad)
//            BLE.USAGE_MINIMUM(1),     0xE0.toByte(),           //   USAGE_MINIMUM (0xE0)
//            BLE.USAGE_MAXIMUM(1),     0xE7.toByte(),           //   USAGE_MAXIMUM (0xE7)
//            BLE.LOGICAL_MINIMUM(1),   0x00,                    //   LOGICAL_MINIMUM (0)
//            BLE.LOGICAL_MAXIMUM(1),   0x01,                    //   Logical Maximum (1)
//            BLE.REPORT_SIZE(1),       0x01,                    //   REPORT_SIZE (1)
//            BLE.REPORT_COUNT(1),      0x08,                    //   REPORT_COUNT (8)
//            BLE.INPUT(1),             0x02,                    //   INPUT (Data,Var,Abs,No Wrap,Linear,Preferred State,No Null Position)
//            BLE.REPORT_COUNT(1),      0x01,                    //   REPORT_COUNT (1) ; 1 byte (Reserved)
//            BLE.REPORT_SIZE(1),       0x08,                    //   REPORT_SIZE (8)
//            BLE.INPUT(1),             0x01,                    //   INPUT (Const,Array,Abs,No Wrap,Linear,Preferred State,No Null Position)
//            BLE.REPORT_COUNT(1),      0x05,                    //   REPORT_COUNT (5) ; 5 bits (Num lock, Caps lock, Scroll lock, Compose, Kana)
//            BLE.REPORT_SIZE(1),       0x01,                    //   REPORT_SIZE (1)
//            BLE.USAGE_PAGE(1),        0x08,                    //   USAGE_PAGE (LEDs)
//            BLE.USAGE_MINIMUM(1),     0x01,                    //   USAGE_MINIMUM (0x01) ; Num Lock
//            BLE.USAGE_MAXIMUM(1),     0x05,                    //   USAGE_MAXIMUM (0x05) ; Kana
//            BLE.OUTPUT(1),            0x02,                    //   OUTPUT (Data,Var,Abs,No Wrap,Linear,Preferred State,No Null Position,Non-volatile)
//            BLE.REPORT_COUNT(1),      0x01,                    //   REPORT_COUNT (1) ; 3 bits (Padding)
//            BLE.REPORT_SIZE(1),       0x03,                    //   REPORT_SIZE (3)
//            BLE.OUTPUT(1),            0x01,                    //   OUTPUT (Const,Array,Abs,No Wrap,Linear,Preferred State,No Null Position,Non-volatile)
//            BLE.REPORT_COUNT(1),      0x06,                    //   REPORT_COUNT (6) ; 6 bytes (Keys)
//            BLE.REPORT_SIZE(1),       0x08,                    //   REPORT_SIZE(8)
//            BLE.LOGICAL_MINIMUM(1),   0x00,                    //   LOGICAL_MINIMUM(0)
//            BLE.LOGICAL_MAXIMUM(1),   0x65,                    //   LOGICAL_MAXIMUM(0x65) ; 101 keys
//            BLE.USAGE_PAGE(1),        0x07,                    //   USAGE_PAGE (Kbrd/Keypad)
//            BLE.USAGE_MINIMUM(1),     0x00,                    //   USAGE_MINIMUM (0)
//            BLE.USAGE_MAXIMUM(1),     0x65,                    //   USAGE_MAXIMUM (0x65)
//            BLE.INPUT(1),             0x00,                    //   INPUT (Data,Array,Abs,No Wrap,Linear,Preferred State,No Null Position)
//            BLE.END_COLLECTION(0)
//        )
//
//        val key_map = arrayOf(
//            byteArrayOf(0x2c, 0),     /*   */
//            byteArrayOf(0x1e, 1),     /* ! */
//            byteArrayOf(0x34, 1),     /* " */
//            byteArrayOf(0x20, 1),     /* # */
//            byteArrayOf(0x21, 1),     /* $ */
//            byteArrayOf(0x22, 1),     /* % */
//            byteArrayOf(0x24, 1),     /* & */
//            byteArrayOf(0x34, 0),     /* ' */
//            byteArrayOf(0x26, 1),     /* ( */
//            byteArrayOf(0x27, 1),     /* ) */
//            byteArrayOf(0x25, 1),     /* * */
//            byteArrayOf(0x2e, 1),     /* + */
//            byteArrayOf(0x36, 0),     /* , */
//            byteArrayOf(0x2d, 0),     /* - */
//            byteArrayOf(0x37, 0),     /* . */
//            byteArrayOf(0x38, 0),     /* / */
//            byteArrayOf(0x27, 0),     /* 0 */
//            byteArrayOf(0x1e, 0),     /* 1 */
//            byteArrayOf(0x1f, 0),     /* 2 */
//            byteArrayOf(0x20, 0),     /* 3 */
//            byteArrayOf(0x21, 0),     /* 4 */
//            byteArrayOf(0x22, 0),     /* 5 */
//            byteArrayOf(0x23, 0),     /* 6 */
//            byteArrayOf(0x24, 0),     /* 7 */
//            byteArrayOf(0x25, 0),     /* 8 */
//            byteArrayOf(0x26, 0),     /* 9 */
//            byteArrayOf(0x33, 1),     /* : */
//            byteArrayOf(0x33, 0),     /* ; */
//            byteArrayOf(0x36, 1),     /* < */
//            byteArrayOf(0x2e, 0),     /* = */
//            byteArrayOf(0x37, 1),     /* > */
//            byteArrayOf(0x38, 1),     /* ? */
//            byteArrayOf(0x1f, 1),     /* @ */
//            byteArrayOf(0x04, 1),     /* A */
//            byteArrayOf(0x05, 1),     /* B */
//            byteArrayOf(0x06, 1),     /* C */
//            byteArrayOf(0x07, 1),     /* D */
//            byteArrayOf(0x08, 1),     /* E */
//            byteArrayOf(0x09, 1),     /* F */
//            byteArrayOf(0x0a, 1),     /* G */
//            byteArrayOf(0x0b, 1),     /* H */
//            byteArrayOf(0x0c, 1),     /* I */
//            byteArrayOf(0x0d, 1),     /* J */
//            byteArrayOf(0x0e, 1),     /* K */
//            byteArrayOf(0x0f, 1),     /* L */
//            byteArrayOf(0x10, 1),     /* M */
//            byteArrayOf(0x11, 1),     /* N */
//            byteArrayOf(0x12, 1),     /* O */
//            byteArrayOf(0x13, 1),     /* P */
//            byteArrayOf(0x14, 1),     /* Q */
//            byteArrayOf(0x15, 1),     /* R */
//            byteArrayOf(0x16, 1),     /* S */
//            byteArrayOf(0x17, 1),     /* T */
//            byteArrayOf(0x18, 1),     /* U */
//            byteArrayOf(0x19, 1),     /* V */
//            byteArrayOf(0x1a, 1),     /* W */
//            byteArrayOf(0x1b, 1),     /* X */
//            byteArrayOf(0x1c, 1),     /* Y */
//            byteArrayOf(0x1d, 1),     /* Z */
//            byteArrayOf(0x2f, 0),     /* [ */
//            byteArrayOf(0x31, 0),     /* \ */
//            byteArrayOf(0x30, 0),     /* ] */
//            byteArrayOf(0x23, 1),     /* ^ */
//            byteArrayOf(0x2d, 1),     /* _ */
//            byteArrayOf(0x53, 0),     /* ` */
//            byteArrayOf(0x04, 0),     /* a */
//            byteArrayOf(0x05, 0),     /* b */
//            byteArrayOf(0x06, 0),     /* c */
//            byteArrayOf(0x07, 0),     /* d */
//            byteArrayOf(0x08, 0),     /* e */
//            byteArrayOf(0x09, 0),     /* f */
//            byteArrayOf(0x0a, 0),     /* g */
//            byteArrayOf(0x0b, 0),     /* h */
//            byteArrayOf(0x0c, 0),     /* i */
//            byteArrayOf(0x0d, 0),     /* j */
//            byteArrayOf(0x0e, 0),     /* k */
//            byteArrayOf(0x0f, 0),     /* l */
//            byteArrayOf(0x10, 0),     /* m */
//            byteArrayOf(0x11, 0),     /* n */
//            byteArrayOf(0x12, 0),     /* o */
//            byteArrayOf(0x13, 0),     /* p */
//            byteArrayOf(0x14, 0),     /* q */
//            byteArrayOf(0x15, 0),     /* r */
//            byteArrayOf(0x16, 0),     /* s */
//            byteArrayOf(0x17, 0),     /* t */
//            byteArrayOf(0x18, 0),     /* u */
//            byteArrayOf(0x19, 0),     /* v */
//            byteArrayOf(0x1a, 0),     /* w */
//            byteArrayOf(0x1b, 0),     /* x */
//            byteArrayOf(0x1c, 0),     /* y */
//            byteArrayOf(0x1d, 0),     /* z */
//            byteArrayOf(0x2f, 1),     /* { */
//            byteArrayOf(0x31, 1),     /* | */
//            byteArrayOf(0x30, 1),     /* } */
//            byteArrayOf(0x53, 1),     /* ~ */
//        )
//
//        fun getKeyCode(c: Char): ByteArray
//        {
//            val keycode = ByteArray(8)
//            val keyset = key_map[c.code - 20]
//            keycode[2] = keyset[0]
//            if (keyset[1].toInt() != 0)
//                keycode[0] = 0x02
//            return keycode
//        }
//    }
//}
