package com.github.asforest.blew.ble;

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.os.Handler
import android.os.ParcelUuid
import android.util.Log


class HidPeripheral(val context: Context)
{
    val Tag = "APP"

    val handler = Handler(context.mainLooper)
    val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    val adapter: BluetoothAdapter = manager.adapter
    val advertiser: BluetoothLeAdvertiser = setupAdvertiser()
    val gattCallback = GattServerCallback(this)
    val gattServer: BluetoothGattServer = manager.openGattServer(context, gattCallback)
    var onlineDevices: MutableMap<String, BluetoothDevice> = mutableMapOf()
    var currentDevice: BluetoothDevice? = null
    val hid: BLEHIDDevices = BLEHIDDevices(gattServer, gattCallback, currentDevice)

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

    /**
     * 创建广播
     */
    fun setupAdvertiser(): BluetoothLeAdvertiser
    {
        if (!adapter.isEnabled)
            throw UnsupportedOperationException("Bluetooth is disabled.")

//        if (!adapter.isMultipleAdvertisementSupported)
//            throw UnsupportedOperationException("Bluetooth LE Advertising not supported on this device.")

        return adapter.bluetoothLeAdvertiser
    }

    /**
     * Starts advertising
     */
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
                .addServiceUuid(ParcelUuid.fromString(BLE.SERVICE_BLE_HID_0x1812.toString()))
//                .addServiceUuid(ParcelUuid.fromString(BLEUUID.SERVICE_DEVICE_INFORMATION_0x180A.toString()))
                .build()

            advertiser.startAdvertising(advertiseSettings, advertiseData, advertiseCallback)
        }
    }

    /**
     * Stops advertising
     */
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

    val advertiseCallback: AdvertiseCallback = object : AdvertiseCallback() {
        override fun onStartFailure(errorCode: Int)
        {
            super.onStartFailure(errorCode)
            throw IllegalStateException("广播启动失败: errorCode: $errorCode")
        }
    }

    fun sendMsg(msg :String)
    {
        if (currentDevice == null)
        {
            Log.i(Tag, "no connection found")
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
