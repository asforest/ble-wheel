package com.example.w2.hid;

import android.bluetooth.*
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.os.Handler
import android.os.ParcelUuid
import android.util.Log
import com.example.w2.hid.UUIDs.*
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue


class HidPeripheral : HidBase
{
    val TAG = "APP__"

    val manufacturer = "kshoji.jp"
    val deviceName = "BLE HID"
    val serialNumber = "12345678"

    var appContent: Context
    var handler: Handler
    lateinit var bluetoothLeAdvertiser: BluetoothLeAdvertiser
    lateinit var inputReportCharacteristic: BluetoothGattCharacteristic
    lateinit var outputReportCharacteristic: BluetoothGattCharacteristic
    lateinit var featureReportCharacteristic: BluetoothGattCharacteristic
    var gattServer: BluetoothGattServer? = null
    var bluetoothDevicesMap: Map<String, BluetoothDevice> = HashMap()

    var bluetoothManager: BluetoothManager
    var bluetoothAdapter: BluetoothAdapter

    var gattServerCallback: BluetoothGattServerCallback
    var currentCentral: BluetoothDevice? = null

    fun checkEnv()
    {
        if (!bluetoothAdapter.isEnabled)
            throw UnsupportedOperationException("Bluetooth is disabled.")

        Log.d(TAG, "isMultipleAdvertisementSupported:" + bluetoothAdapter.isMultipleAdvertisementSupported)
        if (!bluetoothAdapter.isMultipleAdvertisementSupported)
            throw UnsupportedOperationException("Bluetooth LE Advertising not supported on this device.")

        bluetoothLeAdvertiser = bluetoothAdapter.bluetoothLeAdvertiser
        Log.d(TAG, "bluetoothLeAdvertiser: $bluetoothLeAdvertiser")
        if (bluetoothLeAdvertiser == null)
            throw UnsupportedOperationException("Bluetooth LE Advertising not supported on this device.")
    }

    constructor(context: Context, needInputReport: Boolean, needOutputReport: Boolean, needFeatureReport: Boolean, dataSendingRate: Int)
    {
        appContent = context
        handler = Handler(context.mainLooper)
        bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        gattServerCallback = GattServerCallback(this)

        checkEnv()

        gattServer = bluetoothManager.openGattServer(appContent, gattServerCallback)
        if (gattServer == null)
            throw UnsupportedOperationException("gattServer is null, check Bluetooth is ON.")

        // setup services
        addService(setUpHidService(needInputReport, needOutputReport, needFeatureReport))

        // send report each dataSendingRate, if data available
        Timer().scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                val polled = inputReportQueue.poll()
                if (polled != null && inputReportCharacteristic != null)
                {
                    inputReportCharacteristic.value = polled
                    handler.post {
                        val devices: Set<BluetoothDevice> = getDevices()
                        for (device in devices) {
                            if (gattServer != null)
                                gattServer!!.notifyCharacteristicChanged(device, inputReportCharacteristic, false)
                        }
                    }
                }
            }
        }, 0, dataSendingRate.toLong())
    }

//    protected abstract fun getReportMap(): ByteArray?

    /**
     * HID Input Report
     */
    private val inputReportQueue: Queue<ByteArray> = ConcurrentLinkedQueue()
    protected fun addInputReport(inputReport: ByteArray)
    {
        if (inputReport != null && inputReport.isNotEmpty())
            inputReportQueue.offer(inputReport)
    }

    /**
     * HID Output Report
     *
     * @param outputReport the report data
     */
//    protected abstract fun onOutputReport(outputReport: ByteArray)

    /**
     * Add GATT service to gattServer
     *
     * @param service the service
     */
    fun addService(service: BluetoothGattService)
    {
        gattServer!!.addService(service)
        Log.d(TAG, "Service: " + service.uuid + " added.")
    }

    /**
     * Setup HID Service
     *
     * @param isNeedInputReport true: serves 'Input Report' BLE characteristic
     * @param isNeedOutputReport true: serves 'Output Report' BLE characteristic
     * @param isNeedFeatureReport true: serves 'Feature Report' BLE characteristic
     * @return the service
     */
    fun setUpHidService(isNeedInputReport: Boolean, isNeedOutputReport: Boolean, isNeedFeatureReport: Boolean): BluetoothGattService
    {
        val service = BluetoothGattService(SERVICE_BLE_HID_0x1812, BluetoothGattService.SERVICE_TYPE_PRIMARY)

        val PT_READ = BluetoothGattCharacteristic.PROPERTY_READ
        val PT_WRITE = BluetoothGattCharacteristic.PROPERTY_WRITE
        val PT_WRITE_NR = BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE
        val PT_NOTIFY = BluetoothGattCharacteristic.PROPERTY_NOTIFY
        val PM_R = BluetoothGattCharacteristic.PERMISSION_READ
        val PM_W = BluetoothGattCharacteristic.PERMISSION_WRITE
        val PM_RE = BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED
        val PM_WE = BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED

        val char_HidInformation = BluetoothGattCharacteristic(CHARACTERISTIC_HID_INFORMATION_0x2A4A, PT_READ, PM_R)
        val char_HidReportMap = BluetoothGattCharacteristic(CHARACTERISTIC_HID_REPORT_MAP_0x2A4B, PT_READ, PM_R)
        val char_HidControlPoint = BluetoothGattCharacteristic(CHARACTERISTIC_HID_CONTROL_POINT_0x2A4C, PT_WRITE_NR, PM_W)
        val char_HidProtocolMode = BluetoothGattCharacteristic(CHARACTERISTIC_HID_PROTOCOL_MODE_0x2A4E, PT_WRITE_NR or PT_READ, PM_R or PM_W)

        service.addCharacteristic(char_HidInformation)
        service.addCharacteristic(char_HidReportMap)
        service.addCharacteristic(char_HidControlPoint)
        service.addCharacteristic(char_HidProtocolMode)

//        char_HidInformation.value = byteArrayOf(
//            0x11, 0x01,     // HID spec v1.11
//            0x00,           // country code 00
//            0x03            // flags: normally_connectable: 0:off 1:on, remote_wake- 0:off 1:on
//        )
//        char_HidReportMap.value = HID_RM

        // Input Report
        if (isNeedInputReport)
        {
            val characteristic = BluetoothGattCharacteristic(CHARACTERISTIC_HID_REPORT_0x2A4D, PT_READ or PT_NOTIFY, PM_RE or PM_WE)

            val inputReportDescriptor = BluetoothGattDescriptor(DESCRIPTOR_REPORT_REFERENCE_0x2908, PM_RE or PM_WE)
            val CCC2902Descriptor = BluetoothGattDescriptor(DESCRIPTOR_CLIENT_CHARACTERISTIC_CONFIGURATION_0x2902, PM_RE or PM_WE)

//            inputReportDescriptor.value = byteArrayOf(KEYBOARD_ID, 0x01)
            characteristic.addDescriptor(CCC2902Descriptor)
            characteristic.addDescriptor(inputReportDescriptor)

            service.addCharacteristic(characteristic)
            inputReportCharacteristic = characteristic
        }

        // Output Report
        if (isNeedOutputReport)
        {
            val characteristic = BluetoothGattCharacteristic(CHARACTERISTIC_HID_REPORT_0x2A4D, PT_READ or PT_WRITE or PT_WRITE_NR, PM_RE or PM_WE)
            val outputReportDescriptor = BluetoothGattDescriptor(DESCRIPTOR_REPORT_REFERENCE_0x2908, PM_RE or PM_WE)

//            outputReportDescriptor.value = byteArrayOf(KEYBOARD_ID, 0x02)
            characteristic.addDescriptor(outputReportDescriptor)

            service.addCharacteristic(characteristic)
            outputReportCharacteristic = characteristic
        }

        // Feature Report
        if (isNeedFeatureReport)
        {
            val characteristic = BluetoothGattCharacteristic(CHARACTERISTIC_HID_REPORT_0x2A4D, PT_READ or PT_WRITE, PM_RE or PM_WE)
            val featureReportDescriptor = BluetoothGattDescriptor(DESCRIPTOR_REPORT_REFERENCE_0x2908, PM_R or PM_W)

//            featureReportDescriptor.value = byteArrayOf(KEYBOARD_ID, 0x03)

            characteristic.addDescriptor(featureReportDescriptor)
            service.addCharacteristic(characteristic)
            featureReportCharacteristic = characteristic
        }

        return service
    }

    /**
     * Starts advertising
     */
    fun startAdvertising()
    {
        handler.post { // set up advertising setting
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
                .addServiceUuid(ParcelUuid.fromString(SERVICE_BLE_HID_0x1812.toString()))
                .build()

            bluetoothLeAdvertiser.startAdvertising(advertiseSettings, advertiseData, advertiseCallback)


        }
    }

    /**
     * Stops advertising
     */
    fun stopAdvertising()
    {
        handler.post {
            try {
                bluetoothLeAdvertiser.stopAdvertising(advertiseCallback)
            } catch (ignored: IllegalStateException) {
                // BT Adapter is not turned ON
            }
            try {
                if (gattServer != null) {
                    val devices: Set<BluetoothDevice> = getDevices()
                    for (device in devices) {
                        gattServer!!.cancelConnection(device)
                    }
                    gattServer!!.close()
                    gattServer = null
                }
            } catch (ignored: IllegalStateException) {
                // BT Adapter is not turned ON
            }
        }
    }

    /**
     * Callback for BLE connection<br></br>
     * nothing to do.
     */
    private val advertiseCallback: AdvertiseCallback = NullAdvertiseCallback()

    private class NullAdvertiseCallback : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings)
        {
            super.onStartSuccess(settingsInEffect)
            Log.i("APP__", "onStartSuccess: Adv")
        }

        override fun onStartFailure(errorCode: Int)
        {
            super.onStartFailure(errorCode)
            Log.i("APP__", "onStartFailure: Adv")
        }
    }

    /**
     * Obtains connected Bluetooth devices
     *
     * @return the connected Bluetooth devices
     */
    fun getDevices(): Set<BluetoothDevice>
    {
        val deviceSet: MutableSet<BluetoothDevice> = HashSet()
        synchronized(bluetoothDevicesMap) { deviceSet.addAll(bluetoothDevicesMap.values) }
        return Collections.unmodifiableSet(deviceSet)
    }

    fun sendMsg(msg :String)
    {
        if (currentCentral == null) {
            Log.d(TAG, "no connected and register device")
            return
        }

//        msg = mKeyboard.swapCase(msg)

        val msgArray = msg.toCharArray()

        for (cnt_i in msg.indices)
        {
            Log.i(TAG, "KeyCode: ${msgArray[cnt_i]}")

//            val keycode: ByteArray = Keycode.getKeyCode(msgArray[cnt_i])
            keycode = msgArray[cnt_i]
            inputReportCharacteristic.value = keycode
            gattServer!!.notifyCharacteristicChanged(currentCentral, inputReportCharacteristic, false)

            Thread.sleep(20)

            inputReportCharacteristic.value = CLEAN_KEY_REPORT
            gattServer!!.notifyCharacteristicChanged(currentCentral, inputReportCharacteristic, false)

            Thread.sleep(20)
        }
    }



}
