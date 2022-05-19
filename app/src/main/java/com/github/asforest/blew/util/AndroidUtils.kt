package com.github.asforest.blew.util

import android.content.Context
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentActivity
import com.tbruyelle.rxpermissions3.RxPermissions
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object AndroidUtils
{
    fun Context.toast(message: String)
    {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    suspend fun FragmentActivity.requestPermission(permission: String): Boolean
    {
        var continuation: Continuation<Boolean>? = null

        val rxPermissions = RxPermissions(this)
        rxPermissions.request(permission).subscribe { granted -> continuation?.resume(granted) }

//        if (checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED)
//        {
//            continuation?.resume(true)
//        } else if (shouldShowRequestPermissionRationale(permission)) {
//            toast("拒绝了权限申请")
//        } else {
//            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
//                continuation?.resume(isGranted)
//                println("#######################################################################################")
//            }.launch(permission)
//        }

        return suspendCoroutine {
            val isReIn = continuation != null
            continuation = it
            if (isReIn)
                return@suspendCoroutine
        }
    }

    fun Context.popupDialog(title: String, message: String, onClick: (() -> Unit)? = null)
    {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setNegativeButton("好耶") { _, _ -> if (onClick != null) onClick() }
            .setOnDismissListener { onClick }
            .show()
    }
}