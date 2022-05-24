package com.github.asforest.blew.ble

import android.bluetooth.BluetoothGattServer
import com.github.asforest.blew.service.BLEGattServerService


abstract class HIDPeripheral(
    gattServer: BluetoothGattServer,
    bleGattServerService: BLEGattServerService
) {
    val hid: BLEHIDDevices = BLEHIDDevices(gattServer, bleGattServerService)
}
