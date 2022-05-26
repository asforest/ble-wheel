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

        // 请求外部存储读写权限
        viewModel.viewModelScope.launch {
            if (hasPermissions(permissions).isNotEmpty())
            {
                val isGranted = this@MainActivity.requestPermission(permissions)
                if (isGranted.isNotEmpty())
                {
                    popupDialogSuspend("没有权限", "请授权外部存储设备的读写权限以保存配置文件(/sdcard/blew.json)！请在设置中手动勾选\"存储\"权限")
                    finish()
                }
            }
            
            if (!bluetoothManager.adapter.isEnabled)
            {
                registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                    if (it.resultCode == Activity.RESULT_CANCELED)
                        toast("需要开启蓝牙")
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

                toast("BLE广播正在启动\n（在通知栏查看）")
            } else {
                toast("不能重复启动\n（在通知栏查看）")
            }
        }

        stopdown.setOnClickListener {
            if (BLEGattServerService.isRunning)
            {
                stopService(Intent(this@MainActivity, BLEGattServerService::class.java))
                toast("BLE广播正在停止")
            } else {
                toast("别点了，已经停止了")
            }
        }

        val devices = bluetoothManager.adapter.bondedDevices.map { DevicesListAdapter.BtDevice(it.name, it.address) }
        devicesList.adapter = DevicesListAdapter(devices, this)
        devicesList.layoutManager = LinearLayoutManager(this)

        // 绑定服务
        if (BLEGattServerService.isRunning)
            bindToService()
    }

    private fun bindToService()
    {
        // 绑定服务
        val isBound = bindService(Intent(this, BLEGattServerService::class.java), connection , Context.BIND_IMPORTANT)

        if(!isBound)
            popupDialog("出现内部错误", "Service绑定失败")
    }

    private fun createNotificationChannel()
    {
        val id = getString(R.string.notification_channel_name)
        val name = "BLE广播服务"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(id, name, importance)
        channel.description = "BLE GATT Server的广播服务"

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
            holder.deviceConnect.text = "连接"
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
                        mainActivity.popupDialog("无法连接", "因为BLE广播未启动")
                        return@setOnClickListener
                    }

                    mainActivity.toast("正在尝试连接(自动模式)")

                    val d = service!!.adapter.getRemoteDevice(deviceAddress)
                    service!!.gattServer.cancelConnection(d)
                    service!!.gattServer.connect(d, true)
                }

                deviceConnect.setOnLongClickListener {
                    if (service == null)
                    {
                        mainActivity.popupDialog("无法连接", "因为BLE广播未启动")
                        return@setOnLongClickListener true
                    }

                    mainActivity.toast("正在尝试连接(手动模式)")

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