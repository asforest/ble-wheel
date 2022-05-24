package com.github.asforest.blew.activity

import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.asforest.blew.R
import com.github.asforest.blew.service.BLEGattServerService
import com.github.asforest.blew.util.AndroidUtils.popupDialog
import com.github.asforest.blew.util.AndroidUtils.requestPermission
import com.github.asforest.blew.util.AndroidUtils.toast
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity()
{
    object viewModel : ViewModel()

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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
                    startService(Intent(this@MainActivity, BLEGattServerService::class.java))
                    toast("BLE 广播正在启动...")
                }

                finish()
            }
        }
    }
}