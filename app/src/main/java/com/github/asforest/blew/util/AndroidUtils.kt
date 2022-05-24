package com.github.asforest.blew.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.github.asforest.blew.R
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object AndroidUtils
{
    fun Context.toast(message: String)
    {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * 申请权限
     * @param permissions 要申请的权限
     * @return 未能申请到的权限们。如果全部申请成功，返回一个空列表
     */
    suspend fun FragmentActivity.requestPermission(permissions: List<String>): List<String>
    {
        var continuation: Continuation<List<String>>? = null

        val notAcquired = permissions.filter { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }

        if (notAcquired.isEmpty())
            return listOf()

        val denied = permissions.filter { shouldShowRequestPermissionRationale(it) }

        if (denied.isNotEmpty())
            return denied

        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { isGranted ->
            continuation!!.resume(isGranted.filter { !it.value }.map { it.key })
        }.launch(permissions.toTypedArray())

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

    suspend fun Activity.popupInputDialog(title: String, defaultText: String): String
    {
        var continuation: Continuation<String>? = null

        val view = this.layoutInflater.inflate(R.layout.dialog_input, null)
        val titleText = view.findViewById<TextView>(R.id.dialog_title)
        val inputField = view.findViewById<EditText>(R.id.dialog_input)

        titleText.text = title
        inputField.setText(defaultText)

        AlertDialog.Builder(this)
            .setView(view)
            .setPositiveButton("好耶") { dialog, which ->
                val text = (dialog as AlertDialog).findViewById<EditText>(R.id.dialog_input)!!
                continuation?.resume(text.text.toString())
            }.show()

        return suspendCoroutine { continuation = it }
    }

    suspend fun Activity.popupAdvPragmaDialog(steeringHalfConstraint: Int, acceleratorHalfConstraint: Int, reportPeriodMs: Int): List<Int?>
    {
        var continuation: Continuation<List<Int?>>? = null

        val view = this.layoutInflater.inflate(R.layout.dialog_adv_params, null)
        val _steeringHalfConstraint = view.findViewById<EditText>(R.id.steering_half_constraint)
        val _acceleratorHalfConstraint = view.findViewById<EditText>(R.id.accelerator_half_constraint)
        val _reportPeriodMs = view.findViewById<EditText>(R.id.report_period_ms)

        _steeringHalfConstraint.setText(steeringHalfConstraint.toString())
        _acceleratorHalfConstraint.setText(acceleratorHalfConstraint.toString())
        _reportPeriodMs.setText(reportPeriodMs.toString())

        AlertDialog.Builder(this)
            .setView(view)
            .setPositiveButton("好耶") { dialog, which ->
                continuation?.resume(listOf(
                    _steeringHalfConstraint.text.toString().toIntOrNull(),
                    _acceleratorHalfConstraint.text.toString().toIntOrNull(),
                    _reportPeriodMs.text.toString().toIntOrNull(),
                ))
            }.show()

        return suspendCoroutine { continuation = it }
    }


}