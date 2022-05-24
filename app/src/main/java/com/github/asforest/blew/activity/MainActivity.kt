package com.github.asforest.blew.activity

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.asforest.blew.R
import com.github.asforest.blew.service.BLEGattServerService
import com.github.asforest.blew.util.AndroidUtils.popupDialog
import com.github.asforest.blew.util.AndroidUtils.requestPermission
import com.github.asforest.blew.util.AndroidUtils.toast
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity()
{
    object viewModel : ViewModel()
    val notificationManager by lazy { getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        createNotificationChannel()

        // 请求外部存储读写权限
        viewModel.viewModelScope.launch {
            val isGranted = requestPermission(listOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE))
            if (isGranted.isNotEmpty())
            {
                popupDialog("没有权限", "请授权外部存储设备的读、写权限！") {
                    finish()
                }
            } else {
                if (!BLEGattServerService.isRunning)
                {
                    startForegroundService(Intent(this@MainActivity, BLEGattServerService::class.java))
                    toast("BLE广播正在启动\n（在通知栏查看）")
                } else {
                    toast("BLE广播服务已经启动了\n（在通知栏查看）")
                }

                delay(1500)
                finish()
            }
        }
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
}