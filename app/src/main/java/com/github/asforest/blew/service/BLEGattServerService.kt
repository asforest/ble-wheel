package com.github.asforest.blew.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Binder
import android.os.IBinder
import android.os.ParcelUuid
import android.util.Log
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.core.app.NotificationCompat
import com.github.asforest.blew.R
import com.github.asforest.blew.activity.DrivingActivity
import com.github.asforest.blew.activity.MainActivity
import com.github.asforest.blew.ble.BLE
import com.github.asforest.blew.ble.impl.HIDGamepad
import com.github.asforest.blew.event.Event
import com.github.asforest.blew.util.AndroidUtils.toast
import kotlin.random.Random

class BLEGattServerService : Service()
{
    val notificationManager by lazy { getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }
    val btManager: BluetoothManager by lazy { getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager }
    val adapter: BluetoothAdapter by lazy { btManager.adapter }
    val advertiser: BluetoothLeAdvertiser by lazy { adapter.bluetoothLeAdvertiser }
    val gattCallback by lazy { GattServerCallback(this) }
    val advertiseCallback: AdvertiseCallback = AdvCallback(this)
    val gattServer: BluetoothGattServer by lazy { btManager.openGattServer(this, gattCallback) }
    var onlineDevices: MutableMap<String, BluetoothDevice> = mutableMapOf()
    var currentDevice: BluetoothDevice? = null

    var state = ServiceState.Starting
    val ntfActionReceiver by lazy { NotificationActionReceiver(this) }
    val hidGamepad by lazy { setupHIDGamepad() }

    val onBleAdvertiseStart = Event<Int?>()
    val onDeviceConnectionStateChangeEvent = Event<Boolean>()
    val onServiceAddEvent = Event<BluetoothGattService>()
    val onCharacteristicReadEvent = Event<CharacteristicReadEvent>()
    val onCharacteristicWriteEvent = Event<CharacteristicWriteEvent>()
    val onDescriptorReadEvent = Event<DescriptorReadEvent>()
    val onDescriptorWriteEvent = Event<DescriptorWriteEvent>()

    override fun onBind(intent: Intent): IBinder
    {
        return ServiceBinder(this)
    }

    override fun onCreate()
    {
        isRunning = true

        startForeground(NOTIFICATION_ID, buildNotification())

        try {
            // ??????????????????
            registerReceiver(ntfActionReceiver, IntentFilter(ntfActionReceiver.ACTION))

            // ?????????????????????
            hidGamepad.setAccelerator(0)
            hidGamepad.setBrake(0)
            hidGamepad.setSteering(0)

            // ????????????
            onBleAdvertiseStart.once {
                if (it != null)
                {
                    onError("BLE??????????????????", "error code: $it")
                } else {
                    state = ServiceState.Started
                    showOrUpdateMainNotification()
                }
            }

            onDeviceConnectionStateChangeEvent.always {
                showOrUpdateMainNotification()
            }

            // ????????????
            startAdvertising()
        } catch (e: Exception) {
            Log.e("App", e.stackTraceToString())
            onError("????????????", e.stackTraceToString())

            stopSelf()
        }
    }

    override fun onDestroy()
    {
        isRunning = false

        unregisterReceiver(ntfActionReceiver)
        stopAdvertising()
        close()

        toast("BLE GATT Server ????????????")
        Log.i("App", "BLE GATT Server ??????????????????")
    }

    fun setupHIDGamepad(): HIDGamepad
    {
        val config = HIDGamepad.GamepadConfiguration()
        config.controllerType = HIDGamepad.CONTROLLER_TYPE_GAMEPAD
        config.autoReport = false
        config.buttonCount = 128
        config.hatSwitchCount = 0
        config.setWhichAxes(false, false, false, false, false, false, false, false)
        config.setWhichSimulationControls(true, true, true, true, true)

        return HIDGamepad(config, gattServer, this)
    }

    fun showOrUpdateMainNotification()
    {
        notificationManager.notify(NOTIFICATION_ID, buildNotification())
    }

    fun buildNotification(): Notification
    {
        val stopButton = NotificationCompat.Action(
            R.mipmap.ic_launcher, "??????BLE??????",
            PendingIntent.getBroadcast(
                this, 2,
                Intent(ntfActionReceiver.ACTION)
                    .putExtra("button_index", ntfActionReceiver.BUTTON_STOP), 0
            )
        )

        val drivingButton = NotificationCompat.Action(
            R.mipmap.ic_launcher, "??????????????????",
            PendingIntent.getBroadcast(
                this, 3,
                Intent(ntfActionReceiver.ACTION)
                    .putExtra("button_index", ntfActionReceiver.BUTTON_DRIVING), 0
            )
        )

        val openMainActivity = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val contentText = when (state) {
            ServiceState.Starting -> {
                "??????????????????"
            }

            ServiceState.Started -> {
                if (currentDevice == null)
                    "????????????????????????????????????"
                else
                    "${currentDevice!!.name}(${currentDevice!!.address}) ?????????"
            }
        }

        return NotificationCompat.Builder(this, getString(R.string.notification_channel_name))
            .setSmallIcon(R.mipmap.steering_wheel)
            .setContentTitle("BLE GATT Server Advertising Service")
            .setContentText(contentText)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(openMainActivity)
            .addAction(stopButton)
            .addAction(drivingButton)
            .build()
    }

    fun removeMainNotification()
    {
        notificationManager.cancel(NOTIFICATION_ID)
    }

    fun onError(title: String, message: String)
    {
        toast(message)

        val ntf = NotificationCompat.Builder(this, getString(R.string.notification_channel_name))
            .setSmallIcon(R.mipmap.steering_wheel)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle())
            .setPriority(NotificationManager.IMPORTANCE_HIGH)
            .setAllowSystemGeneratedContextualActions(false)
            .build()

        notificationManager.notify(Random.nextInt(), ntf)
    }

    private fun startAdvertising()
    {
        val settings = AdvertiseSettings.Builder()
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_ULTRA_LOW)
            .setConnectable(true)
            .setTimeout(0)
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER)
            .build()

        val data = AdvertiseData.Builder()
            .setIncludeTxPowerLevel(true)
            .setIncludeDeviceName(true)
            .addServiceUuid(ParcelUuid.fromString(BLE.SERVICE_HID_0x1812.toString()))
            .build()

        advertiser.startAdvertising(settings, data, advertiseCallback)
    }

    private fun stopAdvertising()
    {
        advertiser.stopAdvertising(advertiseCallback)
        removeMainNotification()
    }

    private fun close()
    {
        for (device in onlineDevices.values)
            gattServer.cancelConnection(device)

        gattServer.close()
    }

    companion object {
        var isRunning = false

        const val NOTIFICATION_ID = 123123
    }

    enum class ServiceState { Starting, Started }

    class NotificationActionReceiver(val service: BLEGattServerService) : BroadcastReceiver()
    {
        val ACTION = service.packageName + ".NotificationAction"

        val BUTTON_STOP = 2
        val BUTTON_DRIVING = 3

        override fun onReceive(context: Context, intent: Intent)
        {
            when (intent.getIntExtra("button_index", -1))
            {
                BUTTON_STOP -> {
                    service.stopAdvertising()
                    service.stopSelf()
                }

                BUTTON_DRIVING -> {
                    service.startActivity(Intent(service, DrivingActivity::class.java))
                }
            }
        }
    }

    data class ServiceBinder(val service: BLEGattServerService) : Binder()

    class AdvCallback(val ins: BLEGattServerService) : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) { ins.onBleAdvertiseStart.invoke(null) }

        override fun onStartFailure(errorCode: Int) { ins.onBleAdvertiseStart.invoke(errorCode) }
    }

    class GattServerCallback(var ins: BLEGattServerService) : BluetoothGattServerCallback()
    {
        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int)
        {
            Log.d("App", "onConnectionStateChange() device: (${device.name}): $status -> $newState")

            ins.currentDevice = when(newState) {
                BluetoothProfile.STATE_CONNECTED -> if (device.bondState == BluetoothDevice.BOND_BONDED) device else null
                BluetoothProfile.STATE_DISCONNECTED -> null
                else -> null
            }

            ins.onDeviceConnectionStateChangeEvent.invoke(ins.currentDevice != null)
        }

        override fun onServiceAdded(status: Int, service: BluetoothGattService) {
            Log.d("App", "Service Add: ${service.uuid}: $status")
            ins.onServiceAddEvent.invoke(service)
        }

        override fun onCharacteristicReadRequest(device: BluetoothDevice, requestId: Int, offset: Int, characteristic: BluetoothGattCharacteristic)
        {
            Log.d("App", "onCharacteristicReadRequest(): name: (${device.name}), requestId: ($requestId), offset: ($offset), uuid(${characteristic.uuid})")

            ins.onCharacteristicReadEvent.invoke(CharacteristicReadEvent(device, requestId, offset, characteristic))

            ins.gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, characteristic.value)
        }

        override fun onCharacteristicWriteRequest(device: BluetoothDevice, requestId: Int, characteristic: BluetoothGattCharacteristic, preparedWrite: Boolean, responseNeeded: Boolean, offset: Int, value: ByteArray)
        {
            Log.d("App", "onCharacteristicWriteRequest(): name: ${device.name}(${device.address}), requestId: ($requestId), preparedWrite: ($preparedWrite), responseNeeded: ($responseNeeded), offset: ($offset), uuid:(${characteristic.uuid}), value:(${value})")

            ins.onCharacteristicWriteEvent.invoke(CharacteristicWriteEvent(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value))
        }

        override fun onDescriptorReadRequest(device: BluetoothDevice, requestId: Int, offset: Int, descriptor: BluetoothGattDescriptor)
        {
            Log.d("App", "onDescriptorReadRequest() device(${device.name}), requestId($requestId), offset($offset), uuid(${descriptor.uuid})")

            ins.onDescriptorReadEvent.invoke(DescriptorReadEvent(device, requestId, offset, descriptor))

            when(descriptor.uuid)
            {
                BLE.DESCRIPTOR_CLIENT_CHARACTERISTIC_CONFIGURATION_0x2902 -> {
                    val returnValue = if (ins.currentDevice != null && ins.currentDevice == device) {
                        BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    } else {
                        BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
                    }

                    ins.gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, returnValue)
                }

                else -> {
                    ins.gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, descriptor.value)
                }
            }
        }

        override fun onDescriptorWriteRequest(device: BluetoothDevice, requestId: Int, descriptor: BluetoothGattDescriptor, preparedWrite: Boolean, responseNeeded: Boolean, offset: Int, value: ByteArray)
        {
            Log.d("App", "onDescriptorWriteRequest() device(${device.name}), requestId($requestId), offset($offset), uuid(${descriptor.uuid})")

            ins.onDescriptorWriteEvent.invoke(DescriptorWriteEvent(device, requestId, offset, descriptor))
        }
    }

    class CharacteristicWriteEvent(
        val device: BluetoothDevice,
        val requestId: Int,
        val characteristic: BluetoothGattCharacteristic,
        val preparedWrite: Boolean,
        val responseNeeded: Boolean,
        val offset: Int,
        val value: ByteArray
    )

    class DescriptorWriteEvent(
        val device: BluetoothDevice,
        val requestId: Int,
        val offset: Int,
        val descriptor: BluetoothGattDescriptor
    )

    class CharacteristicReadEvent(
        val device: BluetoothDevice,
        val requestId: Int,
        val offset: Int,
        val characteristic: BluetoothGattCharacteristic
    )

    class DescriptorReadEvent(
        val device: BluetoothDevice,
        val requestId: Int,
        val offset: Int,
        val descriptor: BluetoothGattDescriptor
    )
}