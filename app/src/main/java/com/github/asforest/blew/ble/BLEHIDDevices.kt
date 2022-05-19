package com.github.asforest.blew.ble

import android.bluetooth.*
import kotlinx.coroutines.runBlocking
import java.util.*
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.max
import kotlin.math.min

class BLEHIDDevices(
    val gattServer: BluetoothGattServer,
    val hidPeripheral: HIDPeripheral,
) {
    val PT_READ = BluetoothGattCharacteristic.PROPERTY_READ
    val PT_WRITE = BluetoothGattCharacteristic.PROPERTY_WRITE
    val PT_WRITE_NR = BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE
    val PT_NOTIFY = BluetoothGattCharacteristic.PROPERTY_NOTIFY
    val PM_R = BluetoothGattCharacteristic.PERMISSION_READ
    val PM_W = BluetoothGattCharacteristic.PERMISSION_WRITE
    val PM_RE = BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED
    val PM_WE = BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED

    val deviceInfoService = BluetoothGattService(BLE.SERVICE_DEVICE_INFORMATION_0x180A, BluetoothGattService.SERVICE_TYPE_PRIMARY)
    val hidService = BluetoothGattService(BLE.SERVICE_HID_0x1812, BluetoothGattService.SERVICE_TYPE_PRIMARY)
    val batteryService = BluetoothGattService(BLE.SERVICE_BATTERY_0x180F, BluetoothGattService.SERVICE_TYPE_PRIMARY);

    // Mandatory characteristic for device info service
    val pnpCharacteristic = deviceInfoService.createCharacteristic(BLE.CHARACTERISTIC_PNP_0x2A50, PT_READ, PM_R)
    // Optional characteristic for device info service
    val manufacturerNameCharacteristic = deviceInfoService.createCharacteristic(BLE.CHARACTERISTIC_MANUFACTURER_NAME_0x2A29, PT_READ, PM_R)
    val modelNumberCharacteristic = deviceInfoService.createCharacteristic(BLE.CHARACTERISTIC_MODEL_NUMBER_0x2A24, PT_READ, PM_R)
    val serialNumberCharacteristic = deviceInfoService.createCharacteristic(BLE.CHARACTERISTIC_SERIAL_NUMBER_0x2A25, PT_READ, PM_R)

    // Mandatory characteristics for HID service
    val hidInformationCharacteristic = hidService.createCharacteristic(BLE.CHARACTERISTIC_HID_INFORMATION_0x2A4A, PT_READ, PM_R)
    val HidReportMapCharacteristic = hidService.createCharacteristic(BLE.CHARACTERISTIC_HID_REPORT_MAP_0x2A4B, PT_READ, PM_R)
    val HidControlPointCharacteristic = hidService.createCharacteristic(BLE.CHARACTERISTIC_HID_CONTROL_POINT_0x2A4C, PT_WRITE_NR, PM_W)
    val HidProtocolModeCharacteristic = hidService.createCharacteristic(BLE.CHARACTERISTIC_HID_PROTOCOL_MODE_0x2A4E, PT_WRITE_NR or PT_READ, PM_R or PM_W)

    // Mandatory battery level characteristic with notification and presence descriptor
    val batteryLevelCharacteristic = batteryService.createCharacteristic(BLE.CHARACTERISTIC_BATTERY_LEVEL_0x2A19, PT_READ or PT_NOTIFY, PM_R)

    init {
        /*
         * This value is setup here because its default value in most usage cases, its very rare to use boot mode
         * and we want to simplify library using as much as possible
         */
        HidProtocolModeCharacteristic.value = byteArrayOf(0x01)

        // 2902
        val cccDescriptor = batteryLevelCharacteristic.createDescriptor(BLE.DESCRIPTOR_CLIENT_CHARACTERISTIC_CONFIGURATION_0x2902, PM_RE or PM_WE)

        // 2904
        batteryLevelCharacteristic.createDescriptor(BLE.DESCRIPTOR_CLIENT_CHARACTERISTIC_PRESENTATION_0x2904, PT_READ)
        val batteryLevelDescriptor = BLE2904()
        batteryLevelDescriptor.format = BLE2904.FORMAT_UINT8
        batteryLevelDescriptor.namespace = 1u
        batteryLevelDescriptor.unit = (0x27ad).toUShort() // 0x27AD represents percentage
        batteryLevelDescriptor.applyModification()

        batteryLevelCharacteristic.value = byteArrayOf(99)
    }

    /**
     * 此方法在其它参数配置完毕最后调用
     */
    fun startServices()
    {
        val services = listOf(deviceInfoService, hidService, batteryService)

        for (service in services)
            runBlocking { registerService(service) }
    }

    suspend fun registerService(service: BluetoothGattService)
    {
        var continuation: Continuation<Unit>? = null

        hidPeripheral.onServiceAddEvent.once { continuation?.resume(Unit) }
        gattServer.addService(service)

        return suspendCoroutine {
            val isReIn = continuation != null
            continuation = it
            if (isReIn)
                return@suspendCoroutine
        }
    }

    /**
     * Create input report characteristic
     */
    fun inputReport(reportId: Byte): BluetoothGattCharacteristic
    {
        // Create input report characteristic
        val inputReportCharacteristic = hidService.createCharacteristic(BLE.CHARACTERISTIC_HID_REPORT_0x2A4D, PT_READ or PT_NOTIFY, PM_RE)

        // 2902
        val cccDescriptor = inputReportCharacteristic.createDescriptor(BLE.DESCRIPTOR_CLIENT_CHARACTERISTIC_CONFIGURATION_0x2902, PM_RE or PM_WE)
        cccDescriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE

        // 2908
        val reportReferenceDescriptor = inputReportCharacteristic.createDescriptor(BLE.DESCRIPTOR_REPORT_REFERENCE_0x2908, PM_R or PM_RE)
        reportReferenceDescriptor.value = byteArrayOf(reportId, 0x01)

        return inputReportCharacteristic
    }

    /**
     * Create output report characteristic
     */
    fun outputReport(reportId: Byte): BluetoothGattCharacteristic
    {
        val outputReportCharacteristic = hidService.createCharacteristic(BLE.CHARACTERISTIC_HID_REPORT_0x2A4D, PT_READ or PT_WRITE or PT_WRITE_NR, PM_RE or PM_WE)

        // 2908
        val reportReferenceDescriptor = outputReportCharacteristic.createDescriptor(BLE.DESCRIPTOR_REPORT_REFERENCE_0x2908, PM_RE or PM_WE)
        reportReferenceDescriptor.value = byteArrayOf(reportId, 0x02)

        return outputReportCharacteristic
    }

    /**
     * Create feature report characteristic.
     */
    fun featureReport(reportId: Byte): BluetoothGattCharacteristic
    {
        val featureReportCharacteristic = hidService.createCharacteristic(BLE.CHARACTERISTIC_HID_REPORT_0x2A4D, PT_READ or PT_WRITE, PM_RE or PM_WE)
        val featureReportDescriptor = featureReportCharacteristic.createDescriptor(BLE.DESCRIPTOR_REPORT_REFERENCE_0x2908, PM_RE or PM_WE)
        featureReportDescriptor.value = byteArrayOf(reportId, 0x03)

        return featureReportCharacteristic
    }

    /**
     * Set manufacturer name
     */
    fun setManufacturer(name: String)
    {
        manufacturerNameCharacteristic.value = name.toByteArray()
    }

    /**
     * Set device name
     */
    fun setDeviceName(name: String)
    {
        modelNumberCharacteristic.value = name.toByteArray()
    }

    /**
     * Set serial number
     */
    fun setSerialNumber(serial: String)
    {
        serialNumberCharacteristic.value = serial.toByteArray()
    }

    /**
     * Sets the HID Information characteristic value.
     */
    fun hidInfo(country: Byte, flags: Byte)
    {
        hidInformationCharacteristic.value = byteArrayOf(
            0x11, // HID spec v1.11
            0x1,
            country,
            flags // flags: normally_connectable: 0:off 1:on, remote_wake- 0:off 1:on
        )
    }

    /**
     * Set the report map data formatting information.
     */
    fun reportMap(map: ByteArray)
    {
        HidReportMapCharacteristic.value = map
    }

    /**
     * Sets the Plug n Play characteristic value.
     */
    fun pnp(sig: Byte, vid: Short, pid: Short, version: Short)
    {
        pnpCharacteristic.value = byteArrayOf(
            sig,
            (vid.toInt() shr 8).toByte(), vid.toByte(),
            (pid.toInt() shr 8).toByte(), pid.toByte(),
            (version.toInt() shr 8).toByte(), version.toByte()
        )
    }

    /**
     * Set the battery level characteristic value.
     */
    fun setBatteryLevel(level: Byte)
    {
        batteryLevelCharacteristic.value = byteArrayOf(max(0, min(100, level.toInt())).toByte())
        if (hidPeripheral.currentDevice != null)
            gattServer.notifyCharacteristicChanged(hidPeripheral.currentDevice, batteryLevelCharacteristic, true)
    }

    private fun BluetoothGattService.createCharacteristic(uuid: UUID, properties: Int, permissions: Int): BluetoothGattCharacteristic
    {
        val characteristic = BluetoothGattCharacteristic(uuid, properties, permissions)
        addCharacteristic(characteristic)
        return characteristic
    }

    private fun BluetoothGattCharacteristic.createDescriptor(uuid: UUID, permissions: Int): BluetoothGattDescriptor
    {
        val descriptor = BluetoothGattDescriptor(uuid, properties)
        addDescriptor(descriptor)
        return descriptor
    }
}