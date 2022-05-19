package com.github.asforest.blew.activity

import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.w2.R
import com.github.asforest.blew.ble.BLE
import com.github.asforest.blew.ble.HIDPeripheral
import com.github.asforest.blew.ble.impl.HIDGamepad
import com.github.asforest.blew.ble.impl.HIDKeyboard
import kotlin.random.Random

class MainActivity : AppCompatActivity()
{
    lateinit var handler: Handler
    lateinit var devicesList: RecyclerView
//    var keyboard: HIDKeyboard? = null
    var gamepad: HIDGamepad? = null

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        handler = Handler(this.mainLooper)
        devicesList = findViewById(R.id.device_list)

        try {
//            keyboard = HIDKeyboard(applicationContext)
//            keyboard!!.startAdvertising()
//
//            // 监听NumLock状态
//            keyboard!!.gattCallback.onCharacteristicWriteEvent.always {
//                if (it.characteristic.uuid == BLE.CHARACTERISTIC_HID_REPORT_0x2A4D)
//                {
//                    val lock = it.value[0]
//                    Log.i("App", "keyboard lock status update: $lock")
//                }
//            }

            val config = HIDGamepad.GamepadConfiguration()
            config.controllerType = HIDGamepad.CONTROLLER_TYPE_GAMEPAD
            config.autoReport = false
            config.buttonCount = 128
            config.hatSwitchCount = 0
            config.setWhichAxes(false, false, false, false, false, false, false, false)
            config.setWhichSimulationControls(true, true, true, true, true)

            gamepad = HIDGamepad(this, config)
            gamepad?.startAdvertising()
//
            gamepad?.setAccelerator(0)
            gamepad?.setBrake(0)
            gamepad?.setSteering(0)
        } catch (e: Exception) {
            e.printStackTrace()
//            handler.post {
                AlertDialog.Builder(applicationContext).setTitle("BLE启动时发生错误").setMessage(e.message).setNegativeButton("好耶") {_, _ -> }.show()
//            }
//            throw e
//            finish()
        }

//        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
//        val devices = bluetoothManager.adapter.bondedDevices.map {
//            DevicesListAdapter.BtDevice(
//                it.name,
//                it.address
//            )
//        }
//        devicesList.adapter = DevicesListAdapter(devices, keyboard!!)
//        devicesList.layoutManager = LinearLayoutManager(this)
//
        findViewById<Button>(R.id.send).setOnClickListener {
//            keyboard!!.sendMsg("ABC")


        }
    }

    override fun onDestroy()
    {
        super.onDestroy()

//        if(keyboard != null)
//            keyboard!!.stopAdvertising()

        Log.i("APP", "onDestroy: Destroy!")
    }

//    class DevicesListAdapter(val dataSet: List<BtDevice>, val hid: HIDPeripheral): RecyclerView.Adapter<DevicesListAdapter.MyViewHolder>()
//    {
//        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder
//        {
//            val view = LayoutInflater.from(parent.context).inflate(R.layout.device_item, parent, false)
//            return MyViewHolder(view, hid)
//        }
//
//        override fun onBindViewHolder(holder: MyViewHolder, position: Int)
//        {
//            holder.deviceName.text = dataSet[position].name
//            holder.deviceConnect.text = dataSet[position].mac
//            holder.deviceAddress = dataSet[position].mac
//        }
//
//        override fun getItemCount(): Int
//        {
//            return dataSet.size
//        }
//
//        class MyViewHolder(view: View, val hid: HIDPeripheral) : RecyclerView.ViewHolder(view)
//        {
//            var deviceAddress = ""
//
//            var deviceName: TextView = view.findViewById(R.id.device_name)
//            var deviceConnect: Button = view.findViewById(R.id.device_connect)
//
//            init {
//                deviceConnect.setOnClickListener {
//                    Log.i("APP", "正在连接到: ${deviceName.text}(${deviceAddress})")
//                    val d = hid.adapter.getRemoteDevice(deviceAddress)
//                    hid.gattServer!!.cancelConnection(d)
//                    hid.gattServer!!.connect(d, true)
//                }
//
//                deviceConnect.setOnLongClickListener {
//                    Log.i("APP", "正在连接到: ${deviceName.text}(${deviceAddress})")
//                    val d = hid.adapter.getRemoteDevice(deviceAddress)
//                    hid.gattServer!!.cancelConnection(d)
//                    hid.gattServer!!.connect(d, false)
//
//                    true
//                }
//            }
//        }
//
//        class BtDevice(var name: String, var mac: String)
//    }
}