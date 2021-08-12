package com.example.w2

import android.app.AlertDialog
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.DialogInterface
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.w2.hid.HidPeripheral
import java.lang.Exception

class MainActivity : AppCompatActivity()
{
    lateinit var devicesList: RecyclerView
    var hid: HidPeripheral? = null

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        try {
            hid = HidPeripheral(applicationContext, true, true, false, 20)
            hid!!.startAdvertising()
        } catch (e: Exception) {
            Toast.makeText(applicationContext, e.message, Toast.LENGTH_LONG).show()
            finish()
        }

        var bluetoothManager = applicationContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        var bluetoothAdapter = bluetoothManager.adapter

        var devs = ArrayList<DevicesListAdapter.BtDevice>()

        for (dev in bluetoothAdapter.bondedDevices)
        {
            devs.add(DevicesListAdapter.BtDevice(dev.name, dev.address))
        }

        devicesList = findViewById(R.id.device_list)
        devicesList.adapter = DevicesListAdapter(devs, hid!!)
        devicesList.layoutManager = LinearLayoutManager(this)

        findViewById<Button>(R.id.send).setOnClickListener {
            hid!!.sendMsg("ABC")
        }

    }

    override fun onDestroy()
    {
        super.onDestroy()

        if(hid != null)
            hid!!.stopAdvertising()

        Log.i("APP__", "onDestroy: Destroy!")
    }

    class DevicesListAdapter(dataSet: ArrayList<BtDevice>, hid: HidPeripheral): RecyclerView.Adapter<DevicesListAdapter.MyViewHolder>()
    {
        val dataSet: ArrayList<BtDevice> = dataSet
        val hid: HidPeripheral = hid

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder
        {
            var view = LayoutInflater.from(parent.context).inflate(R.layout.device_item, parent, false)
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

        class MyViewHolder(view: View, hid: HidPeripheral) : RecyclerView.ViewHolder(view)
        {
            var hid: HidPeripheral = hid
            var deviceAddress = ""

            var deviceName: TextView = view.findViewById(R.id.device_name)
            var deviceConnect: Button = view.findViewById(R.id.device_connect)

            init {
                deviceConnect.setOnClickListener {
                    Log.i("APP__", "正在连接到: ${deviceName.text}(${deviceAddress})")
                    var d = hid.bluetoothAdapter.getRemoteDevice(deviceAddress)
                    hid.gattServer!!.connect(d, true)
                }

                deviceConnect.setOnLongClickListener {
                    Log.i("APP__", "正在连接到: ${deviceName.text}(${deviceAddress})")
                    var d = hid.bluetoothAdapter.getRemoteDevice(deviceAddress)
                    hid.gattServer!!.connect(d, false)

                    true
                }
            }
        }

        class BtDevice(var name: String, var mac: String)

    }
}