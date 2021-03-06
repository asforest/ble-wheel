package com.github.asforest.blew.activity

import android.Manifest
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.asforest.blew.R
import com.github.asforest.blew.service.BLEGattServerService
import com.github.asforest.blew.util.AndroidUtils.hasPermissions
import com.github.asforest.blew.util.AndroidUtils.popupDialog
import com.github.asforest.blew.util.AndroidUtils.popupDialogSuspend
import com.github.asforest.blew.util.AndroidUtils.requestPermission
import com.github.asforest.blew.util.AndroidUtils.toast
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity()
{
    object viewModel : ViewModel()
    val notificationManager by lazy { getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }
    val bluetoothManager by lazy { getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager }

    val devicesList: RecyclerView by lazy { findViewById(R.id.device_list) }
    val startup: Button by lazy { findViewById(R.id.button_startup) }
    val stopdown: Button by lazy { findViewById(R.id.button_stopdown) }

    var bleService: BLEGattServerService? = null
    var connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            bleService = (service as BLEGattServerService.ServiceBinder).service
        }
        override fun onServiceDisconnected(name: ComponentName) {
            bleService = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        createNotificationChannel()

        val permissions = listOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)

        // ??????????????????????????????
        viewModel.viewModelScope.launch {
            if (hasPermissions(permissions).isNotEmpty())
            {
                val isGranted = this@MainActivity.requestPermission(permissions)
                if (isGranted.isNotEmpty())
                {
                    popupDialogSuspend("????????????", "???????????????????????????????????????????????????????????????(/sdcard/blew.json)??????????????????????????????\"??????\"??????")
                    finish()
                }
            }

            if (!bluetoothManager.adapter.isEnabled)
            {
                registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                    if (it.resultCode == Activity.RESULT_CANCELED)
                        toast("??????????????????")
                }.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            }

            init()
        }
    }

    override fun onDestroy()
    {
        super.onDestroy()

        if (bleService != null)
            unbindService(connection)
    }

    private fun init()
    {
        startup.setOnClickListener {
            if (!BLEGattServerService.isRunning)
            {
                startForegroundService(Intent(this@MainActivity, BLEGattServerService::class.java))

                bindToService()

                toast("BLE??????????????????\n????????????????????????")
            } else {
                toast("??????????????????\n????????????????????????")
            }
        }

        stopdown.setOnClickListener {
            if (BLEGattServerService.isRunning)
            {
                stopService(Intent(this@MainActivity, BLEGattServerService::class.java))
                toast("BLE??????????????????")
            } else {
                toast("???????????????????????????")
            }
        }

        val devices = bluetoothManager.adapter.bondedDevices.map { DevicesListAdapter.BtDevice(it.name, it.address) }
        devicesList.adapter = DevicesListAdapter(devices, this)
        devicesList.layoutManager = LinearLayoutManager(this)

        // ????????????
        if (BLEGattServerService.isRunning)
            bindToService()
    }

    private fun bindToService()
    {
        // ????????????
        val isBound = bindService(Intent(this, BLEGattServerService::class.java), connection , Context.BIND_IMPORTANT)

        if(!isBound)
            popupDialog("??????????????????", "Service????????????")
    }

    private fun createNotificationChannel()
    {
        val id = getString(R.string.notification_channel_name)
        val name = "BLE????????????"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(id, name, importance)
        channel.description = "BLE GATT Server???????????????"

        notificationManager.createNotificationChannel(channel)
    }

    class DevicesListAdapter(val dataSet: List<BtDevice>, val mainActivity: MainActivity)
        : RecyclerView.Adapter<DevicesListAdapter.MyViewHolder>()
    {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder
        {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.device_item, parent, false)
            return MyViewHolder(view, mainActivity)
        }

        override fun onBindViewHolder(holder: MyViewHolder, position: Int)
        {
            holder.deviceName.text = "${dataSet[position].name} (${dataSet[position].mac})"
            holder.deviceConnect.text = "??????"
            holder.deviceAddress = dataSet[position].mac
        }

        override fun getItemCount(): Int
        {
            return dataSet.size
        }

        class MyViewHolder(view: View, val mainActivity: MainActivity) : RecyclerView.ViewHolder(view)
        {
            var deviceAddress = ""
            var deviceName: TextView = view.findViewById(R.id.device_name)
            var deviceConnect: Button = view.findViewById(R.id.device_connect)

            val service get() = mainActivity.bleService

            init {
                deviceConnect.setOnClickListener {
                    if (service == null)
                    {
                        mainActivity.popupDialog("????????????", "??????BLE???????????????")
                        return@setOnClickListener
                    }

                    mainActivity.toast("??????????????????(????????????)")

                    val d = service!!.adapter.getRemoteDevice(deviceAddress)
                    service!!.gattServer.cancelConnection(d)
                    service!!.gattServer.connect(d, true)
                }

                deviceConnect.setOnLongClickListener {
                    if (service == null)
                    {
                        mainActivity.popupDialog("????????????", "??????BLE???????????????")
                        return@setOnLongClickListener true
                    }

                    mainActivity.toast("??????????????????(????????????)")

                    val d = service!!.adapter.getRemoteDevice(deviceAddress)
                    service!!.gattServer.cancelConnection(d)
                    service!!.gattServer.connect(d, false)
                    true
                }
            }
        }

        class BtDevice(var name: String, var mac: String)
    }
}