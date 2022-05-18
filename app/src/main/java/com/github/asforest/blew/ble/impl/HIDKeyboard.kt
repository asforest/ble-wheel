package com.github.asforest.blew.ble.impl

import android.content.Context
import android.util.Log
import com.github.asforest.blew.ble.BLE
import com.github.asforest.blew.ble.HIDPeripheral

class HIDKeyboard(context: Context) : HIDPeripheral(context)
{
    val inputReport = hid.inputReport(BLE.REPORT_ID_KEYBOARD)
    val outputReport = hid.outputReport(BLE.REPORT_ID_KEYBOARD)

    init {
        hid.manufacturer("Asf")
        hid.pnp(0x02, 0x05ac, 0x820a, 0x0210)
        hid.hidInfo(0x00, 0x01) // 第二个参数可以等于0x03
        hid.reportMap(BLE.HID_REPORT_MAP_KEYBOARD)
        hid.batteryLevel(98)

        hid.startServices()
    }

    fun sendMsg(msg :String)
    {
        if (currentDevice == null)
        {
            Log.i("App", "no connection found")
            return
        }

        inputReport.value = byteArrayOf(0, 0, 0x05, 0, 0, 0, 0, 0)
        gattServer.notifyCharacteristicChanged(currentDevice, inputReport, false)

        Thread.sleep(20)

        inputReport.value = BLE.CLEAN_KEY_REPORT
        gattServer.notifyCharacteristicChanged(currentDevice, inputReport, false)

        Thread.sleep(20)

        hid.batteryLevel(50)
    }
}