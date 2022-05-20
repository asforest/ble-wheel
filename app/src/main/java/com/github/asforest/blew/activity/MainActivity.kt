package com.github.asforest.blew.activity

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.asforest.blew.R
import com.github.asforest.blew.ble.BLE
import com.github.asforest.blew.ble.HIDPeripheral
import com.github.asforest.blew.ble.impl.HIDGamepad
import com.github.asforest.blew.util.AndroidUtils.popupDialog
import com.github.asforest.blew.util.AndroidUtils.requestPermission
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity()
{
    object viewModel : ViewModel()
    lateinit var handler: Handler
    lateinit var devicesList: RecyclerView
    var gamepad: HIDGamepad? = null

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        handler = Handler(this.mainLooper)
        devicesList = findViewById(R.id.device_list)

        try {
            val config = HIDGamepad.GamepadConfiguration()
            config.controllerType = HIDGamepad.CONTROLLER_TYPE_GAMEPAD
            config.autoReport = false
            config.buttonCount = 128
            config.hatSwitchCount = 0
            config.setWhichAxes(false, false, false, false, false, false, false, false)
            config.setWhichSimulationControls(true, true, true, true, true)

            gamepad = HIDGamepad(this, config)
            _gamepad = gamepad
            gamepad?.startAdvertising()

            gamepad?.setAccelerator(0)
            gamepad?.setBrake(0)
            gamepad?.setSteering(0)

            gamepad!!.onCharacteristicReadEvent.always {
                if (it.characteristic.uuid == BLE.CHARACTERISTIC_HID_REPORT_MAP_0x2A4B)
                    startActivity(Intent(this@MainActivity, DrivingActivity::class.java))
            }

            findViewById<Button>(R.id.send).setOnClickListener {
                startActivity(Intent(this@MainActivity, DrivingActivity::class.java))
            }

            // 请求外部存储读写权限
            viewModel.viewModelScope.launch {
                val isGranted = requestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                if (!isGranted)
                {
                    popupDialog("没有权限", "请授权外部存储设备的读写权限！") {
                        finish()
                    }
                }
            }

            initDevicesList()
        } catch (e: Exception) {
            popupDialog("启动时发生错误", e.stackTraceToString()) {
                finish()
                throw e
            }
        }
    }

    override fun onDestroy()
    {
        super.onDestroy()

        gamepad?.stopAdvertising()
    }

    fun initDevicesList()
    {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val devices = bluetoothManager.adapter.bondedDevices.map {
            DevicesListAdapter.BtDevice(it.name, it.address)
        }
        devicesList.adapter = DevicesListAdapter(devices, gamepad!!)
        devicesList.layoutManager = LinearLayoutManager(this)
    }

    companion object {
        @JvmStatic
        var _gamepad: HIDGamepad? = null
    }

    class DevicesListAdapter(val dataSet: List<BtDevice>, val hid: HIDPeripheral): RecyclerView.Adapter<DevicesListAdapter.MyViewHolder>()
    {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder
        {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.device_item, parent, false)
            return MyViewHolder(view, hid)
        }

        override fun onBindViewHolder(holder: MyViewHolder, position: Int)
        {
            holder.deviceName.text = dataSet[position].name
            holder.deviceConnect.text = dataSet[position].mac
            holder.deviceAddress = dataSet[position].mac
        }

        override fun getItemCount(): Int
        {
            return dataSet.size
        }

        class MyViewHolder(view: View, val hid: HIDPeripheral) : RecyclerView.ViewHolder(view)
        {
            var deviceAddress = ""

            var deviceName: TextView = view.findViewById(R.id.device_name)
            var deviceConnect: Button = view.findViewById(R.id.device_connect)

            init {
                deviceConnect.setOnClickListener {
                    Log.i("APP", "正在连接到: ${deviceName.text}(${deviceAddress})")
                    val d = hid.adapter.getRemoteDevice(deviceAddress)
                    hid.gattServer.cancelConnection(d)
                    hid.gattServer.connect(d, true)
                }

                deviceConnect.setOnLongClickListener {
                    Log.i("APP", "正在连接到: ${deviceName.text}(${deviceAddress})")
                    val d = hid.adapter.getRemoteDevice(deviceAddress)
                    hid.gattServer.cancelConnection(d)
                    hid.gattServer.connect(d, false)

                    true
                }
            }
        }

        class BtDevice(var name: String, var mac: String)
    }
}