package com.github.asforest.blew.activity

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.BatteryManager
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.asforest.blew.R
import com.github.asforest.blew.ble.impl.HIDGamepad
import com.github.asforest.blew.event.Event
import com.github.asforest.blew.service.BLEGattServerService
import com.github.asforest.blew.util.AndroidUtils.popupAdvPragmaDialog
import com.github.asforest.blew.util.AndroidUtils.popupDialog
import com.github.asforest.blew.util.AndroidUtils.popupInputDialog
import com.github.asforest.blew.util.AndroidUtils.toast
import com.github.asforest.blew.util.FileObj
import kotlinx.coroutines.launch
import org.joml.Math
import org.joml.Matrix4f
import org.joml.Quaternionf
import org.joml.Vector3f
import org.json.JSONObject
import java.lang.Integer.min
import kotlin.math.abs
import kotlin.math.max


class DrivingActivity : AppCompatActivity(), SensorEventListener
{
    object viewModel : ViewModel()
    val handler: Handler by lazy { Handler(mainLooper) }
    val sensorManager: SensorManager by lazy { getSystemService(SENSOR_SERVICE) as SensorManager }
    val rotationSensor: Sensor by lazy { sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) }

    val currentRotationText: TextView by lazy { findViewById(R.id.current_rotation) }
    val referenceRotationText: TextView by lazy { findViewById(R.id.reference_rotation) }
    val primaryButton: Button by lazy { findViewById(R.id.button_center) }
    val accelerator_bar: ProgressBar by lazy { findViewById(R.id.accelerator_bar) }
    val brake_bar: ProgressBar by lazy { findViewById(R.id.brake_bar) }
    val functionalButtons by lazy { arrayOf<Button>(
        findViewById(R.id.gamepad_button_1),
        findViewById(R.id.gamepad_button_2),
        findViewById(R.id.gamepad_button_3),
        findViewById(R.id.gamepad_button_4),
        findViewById(R.id.gamepad_button_5),
        findViewById(R.id.gamepad_button_6),
        findViewById(R.id.gamepad_button_7),
        findViewById(R.id.gamepad_button_8),
        findViewById(R.id.gamepad_button_9),
        findViewById(R.id.gamepad_button_10),
        findViewById(R.id.gamepad_button_11),
        findViewById(R.id.gamepad_button_12),
        findViewById(R.id.gamepad_button_13),
        findViewById(R.id.gamepad_button_14),
        findViewById(R.id.gamepad_button_15),
        findViewById(R.id.gamepad_button_16),
        findViewById(R.id.gamepad_button_17),
        findViewById(R.id.gamepad_button_18),
        findViewById(R.id.gamepad_button_19),
        findViewById(R.id.gamepad_button_20),
        findViewById(R.id.gamepad_button_21),
        findViewById(R.id.gamepad_button_22),
        findViewById(R.id.gamepad_button_23),
        findViewById(R.id.gamepad_button_24),

        findViewById(R.id.gamepad_button_25),
        findViewById(R.id.gamepad_button_26),
        findViewById(R.id.gamepad_button_27),
    ) }

    var editingMode = false
    var steeringHalfConstraint: Int = 270
    var acceleratorHalfConstraint: Int = 45
    var reportPeriodMs: Int = 50

    var reportingEnabled = true // 为false时禁用所有旋转，也不向BLE报告陀螺仪数据
    var currentRotation: Matrix4f? = null
    var referenceRotation: Matrix4f? = null

    var bleService: BLEGattServerService? = null
    val hidGamepad: HIDGamepad? get() = bleService?.hidGamepad

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driving)

        // 绑定服务
        val isBound = bindService(Intent(this, BLEGattServerService::class.java), object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, service: IBinder) {
                bleService = (service as BLEGattServerService.ServiceBinder).service
            }
            override fun onServiceDisconnected(name: ComponentName) {
                bleService = null
                toast("BLE GATT 服务已停止")
                finish()
            }
        }, Context.BIND_IMPORTANT)

        if(!isBound)
        {
            popupDialog("服务绑定失败", "服务绑定失败，点击退出") { finish() }
            return
        }

        // 注册传感器事件
        sensorManager.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_GAME)

        // 监听按钮点击

        // 重新记录下参考值
        primaryButton.setOnLongClickListener {
            if (referenceRotation == null)
                referenceRotation = Matrix4f()
            referenceRotation!!.set(currentRotation)
            referenceRotationText.text = referenceRotation!!.stringify()

            // 复位数据
            accumulatedSteeringAngle = 0f
            accumulatedAcceleratorAngle = 0f
            previousSteeringAngle = null
            previousAcceleratorAngleX = null
            true
        }

        // 启用/禁用数据报告
        primaryButton.setOnClickListener {
            reportingEnabled = !reportingEnabled
            primaryButton.alpha = if (reportingEnabled) 1f else 0.4f
        }

        for ((index, button) in functionalButtons.withIndex())
        {
            button.setOnTouchListener { _, event ->
                if (!editingMode)
                {
                    if (event.action == MotionEvent.ACTION_DOWN)
                    {
                        hidGamepad?.press((index + 1).toUByte())
                        hidGamepad?.sendReport(true)
                    }

                    if (event.action == MotionEvent.ACTION_UP)
                    {
                        hidGamepad?.release((index + 1).toUByte())
                        hidGamepad?.sendReport(true)
                    }
                } else {
                    if (event.action == MotionEvent.ACTION_DOWN)
                    {
                        viewModel.viewModelScope.launch {
                            val newText = popupInputDialog("编辑按钮${index}上的文字", button.text.toString())
                            button.text = newText
                            saveConfig()
                        }
                    }
                }

                true
            }
        }

        // 两个TextView也是按钮
        for ((button, widget) in mapOf(HIDGamepad.BUTTON_125 to currentRotationText,
            HIDGamepad.BUTTON_126 to referenceRotationText))
        {
            widget.setOnTouchListener { _, event ->
                if (!editingMode)
                {
                    if (event.action == MotionEvent.ACTION_DOWN)
                    {
                        hidGamepad?.press(button)
                        hidGamepad?.sendReport(true)
                    }

                    if (event.action == MotionEvent.ACTION_UP)
                    {
                        hidGamepad?.release(button)
                        hidGamepad?.sendReport(true)
                    }
                } else {
                    if (event.action == MotionEvent.ACTION_DOWN)
                    {
                       viewModel.viewModelScope.launch {
                           val newPragma = popupAdvPragmaDialog(steeringHalfConstraint, acceleratorHalfConstraint, reportPeriodMs)
                           val steeringConstraint = Math.clamp(15, 1000, newPragma[0] ?: steeringHalfConstraint)
                           val acceleratorConstraint = Math.clamp(5, 180, newPragma[1] ?: acceleratorHalfConstraint)
                           val reportPeriod = Math.clamp(30, 5000, newPragma[2] ?: reportPeriodMs)

                           steeringHalfConstraint = steeringConstraint
                           acceleratorHalfConstraint = acceleratorConstraint
                           reportPeriodMs = reportPeriod

                           saveConfig()

                           popupDialog("高级参数已更新", "已即时生效")
                       }
                    }
                }

                true
            }
        }

        loadConfig()

        // 定时报告数据
        repeatedlyRun(reportPeriodMs.toLong()) { reportSensorData() }

        // 处理设备断开事件
//        hidGamepad.onDeviceConnectionStateChangeEvent.once {
//            if (!it)
//                finish()
//        }

        // 保持屏幕开启
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // 首次报告电量 + 定时刷新电量
        registerReceiver(batteryLevelChangeReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        onBatteryLevelChangeEvent.always { hidGamepad?.hid?.setBatteryLevel(max(0, min(100, it)).toByte()) }
    }

//    override fun onMenuOpened(featureId: Int, menu: Menu): Boolean
//    {
//
//    }

    override fun onWindowFocusChanged(hasFocus: Boolean)
    {
        super.onWindowFocusChanged(hasFocus)

        if (hasFocus && !editingMode)
            enterFullscreen()
    }

    override fun onDestroy()
    {
        super.onDestroy()
        sensorManager.unregisterListener(this)
        unregisterReceiver(batteryLevelChangeReceiver)
    }

    fun enterFullscreen()
    {
        window.decorView.systemUiVisibility = window.decorView.systemUiVisibility or
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
    }

    fun exitFullscreen()
    {
        window.decorView.systemUiVisibility = window.decorView.systemUiVisibility and (
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY).inv()
    }

    var accumulatedSteeringAngle: Float = 0f // 方向盘当前累计旋转角度（可能超过360）
    var accumulatedAcceleratorAngle: Float = 0f // 油门刹车轴当前累计旋转角度（可能超过360）
    private var previousSteeringAngle: Float? = null // 上一次的位置角度，用来计算角度delta
    private var previousAcceleratorAngleX: Float? = null // 上一次的位置角度，用来计算角度delta
    @SuppressLint("SetTextI18n")
    fun reportSensorData()
    {
        val hasReferenceRotation = referenceRotation != null
        val hasCurrentRotation = currentRotation != null

        if(!hasReferenceRotation || !hasCurrentRotation || !reportingEnabled)
            return

        // 计算传感器偏移值
        val currX = Vector3f()
        val currY = Vector3f()
        val currZ = Vector3f()
        currentRotation!!.getColumn(0, currX)
        currentRotation!!.getColumn(1, currY)
        currentRotation!!.getColumn(2, currZ)

        val refX = Vector3f()
        val refY = Vector3f()
        val refZ = Vector3f()
        referenceRotation!!.getColumn(0, refX)
        referenceRotation!!.getColumn(1, refY)
        referenceRotation!!.getColumn(2, refZ)

        val rotateOnZ = Quaternionf()
        currZ.rotationTo(refZ, rotateOnZ)
        val rotatedY = currY.rotate(rotateOnZ)
        val currentSteeringAngle = Math.toDegrees(rotatedY.angleSigned(refY, refZ).toDouble()).toFloat()

        val rotateOnY = Quaternionf()
        currY.rotationTo(refY, rotateOnY)
        val rotatedX = currX.rotate(rotateOnY)
        val currentAcceleratorAngle = Math.toDegrees(rotatedX.angleSigned(refX, refY).toDouble()).toFloat()

        var reported = false

        /**
         * 报告一次数据
         */
        fun reportOnce()
        {
            if (!reported)
                hidGamepad?.sendReport(false)
            reported = true
        }

        /**
         * 计算角度偏移方向和值（顺时针为正方向）
         */
        fun angleDelta(currentAngle: Float, previousAngle: Float): Float
        {
            val d1 = currentAngle - previousAngle
            val d21 = currentAngle - 360 - previousAngle
            val d22 = currentAngle + 360 - previousAngle
            val d2 = if (abs(d21) < abs(d22)) d21 else d22
            return if (abs(d1) < abs(d2)) d1 else d2
        }

        // 计算方向盘实际角度
        if (previousSteeringAngle != null)
        {
            // 计算方向盘实际朝向
            accumulatedSteeringAngle += angleDelta(currentSteeringAngle, previousSteeringAngle!!)

            // 方向盘的最大旋转角度（一半）
            val halfConstraint = steeringHalfConstraint.toFloat()

            // 归一化（默认位置是在0.5）
            val normalized = (Math.clamp(-halfConstraint, halfConstraint, accumulatedSteeringAngle) + halfConstraint) / (halfConstraint * 2)

            // 转换成HID协议范围的值
            val hidValue = ((65534 * normalized).toInt() - 32768).toShort()

            // 上报
            hidGamepad?.setSteering(hidValue)
            reportOnce()
        }
        previousSteeringAngle = currentSteeringAngle

        var acceleratorBarProgress: Float = 0.5f
        // 计算油门刹车实际角度
        if (previousAcceleratorAngleX != null)
        {
            // 计算方向盘实际朝向
            accumulatedAcceleratorAngle += angleDelta(currentAcceleratorAngle, previousAcceleratorAngleX!!)

            // 最大旋转角度（一半）
            val halfConstraint = acceleratorHalfConstraint.toFloat()

            // 归一化（默认位置是在0.5）
            val normalized = (Math.clamp(-halfConstraint, halfConstraint, accumulatedAcceleratorAngle) + halfConstraint) / (halfConstraint * 2)

            // 转换成HID协议范围的值
            val hidValue = ((65534 * normalized).toInt() - 32768).toShort()

            // 上报
//            hidGamepad.setBrake(hidValue)
//            hidGamepad.setAccelerator((65535 - (hidValue.toInt() + 32768) - 32768).toShort())

//            Log.i("App", "Report Acc: $hidValue")
            hidGamepad?.setAccelerator(hidValue)
            reportOnce()

            acceleratorBarProgress = normalized
        }
        previousAcceleratorAngleX = currentAcceleratorAngle

        // 更新UI
        primaryButton.rotation = -currentSteeringAngle
        primaryButton.text = "Z: ${a(currentSteeringAngle)} / ${a(accumulatedSteeringAngle)}\nX: ${a(currentAcceleratorAngle)} / ${a(accumulatedAcceleratorAngle)}"
        val ab = 200 - (200 * acceleratorBarProgress).toInt()
        accelerator_bar.progress = max(0, min(100, ab - 100))
        brake_bar.progress = max(0, min(100, 100 - ab))
    }

    fun loadConfig()
    {
        val configFile = FileObj("/sdcard/blew.json")

        // 保存默认配置文件
        if (!configFile.exists)
        {
            val root = JSONObject()

            root.put("steering_half_constraint", 270)
            root.put("accelerator_half_constraint", 45)
            root.put("report_period", 50)

            for ((index, button) in functionalButtons.withIndex())
                root.put("b${index + 1}", "b${index + 1}")

            configFile.content = root.toString(4)
        }

        // 加载配置文件
        val root = JSONObject(configFile.content)
        for ((index, button) in functionalButtons.withIndex())
            functionalButtons[index].text = root.optString("b${index + 1}", "b${index + 1}")

        steeringHalfConstraint = root.optInt("steering_half_constraint", 270)
        acceleratorHalfConstraint = root.optInt("accelerator_half_constraint", 45)
        reportPeriodMs = root.optInt("report_period", 50)
    }

    fun saveConfig()
    {
        val configFile = FileObj("/sdcard/blew.json")

        val root = JSONObject()
        root.put("steering_half_constraint", steeringHalfConstraint)
        root.put("accelerator_half_constraint", acceleratorHalfConstraint)
        root.put("report_period", reportPeriodMs)

        for ((index, button) in functionalButtons.withIndex())
            root.put("b${index + 1}", button.text)

        configFile.content = root.toString(4)
    }

    fun queryBatteryLevel(): Int
    {
        val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus = registerReceiver(null, intentFilter)

        val batteryPct: Float? = batteryStatus?.let { intent ->
            val level: Int = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale: Int = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            level * 100 / scale.toFloat()
        }

        return (batteryPct ?: 1f).toInt()
    }

    val onBatteryLevelChangeEvent = Event<Int>()
    val batteryLevelChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != Intent.ACTION_BATTERY_CHANGED)
                return
            val level: Int = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale: Int = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val percent = max(0, min(100, (level * 100 / scale.toFloat()).toInt()))
            onBatteryLevelChangeEvent.invoke(percent)
        }
    }

    fun switchEditingMode()
    {
        editingMode = !editingMode
        if (editingMode)
            exitFullscreen()
        else
            enterFullscreen()
        popupDialog("模式已切换", "已经切换到" + if (editingMode) "编辑模式" else "正常模式")
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean = onKeyEvent(keyCode, true)

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean = onKeyEvent(keyCode, false)

    private val pressedKeys = mutableSetOf<Int>()
    fun onKeyEvent(keyCode: Int, press: Boolean): Boolean
    {
        // 防止重复触发
        if (press && !pressedKeys.add(keyCode))
            return true

        if (!press && !pressedKeys.remove(keyCode))
            return true

        when (keyCode)
        {
            KeyEvent.KEYCODE_VOLUME_UP -> {
                // 进入设置模式
                if (press && KeyEvent.KEYCODE_VOLUME_DOWN in pressedKeys)
                {
                    switchEditingMode()
                } else {
                    if (press)
                        hidGamepad?.press(HIDGamepad.BUTTON_127)
                    else
                        hidGamepad?.release(HIDGamepad.BUTTON_127)
                    hidGamepad?.sendReport(true)
                }
            }

            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                // 进入设置模式
                if (press && KeyEvent.KEYCODE_VOLUME_UP in pressedKeys)
                {
                    switchEditingMode()
                } else {
                    if (press)
                        hidGamepad?.press(HIDGamepad.BUTTON_128)
                    else
                        hidGamepad?.release(HIDGamepad.BUTTON_128)
                    hidGamepad?.sendReport(true)
                }
            }

            KeyEvent.KEYCODE_MENU -> {
                if (press)
                    switchEditingMode()
            }

            KeyEvent.KEYCODE_BACK -> {
                if (press)
                    finish()
            }

            else -> return false
        }

        return true
    }

    private val temp = FloatArray(16)
    @SuppressLint("SetTextI18n")
    override fun onSensorChanged(event: SensorEvent)
    {
        if (event.sensor.type != Sensor.TYPE_ROTATION_VECTOR)
            return

        SensorManager.getRotationMatrixFromVector(temp, event.values)

        if (currentRotation == null)
            currentRotation = Matrix4f()

        currentRotation!!.set(
            temp[0], temp[4], temp[8],   0f,
            temp[1], temp[5], temp[9],   0f,
            temp[2], temp[6], temp[10],  0f,
            0f,      0f,      0f,        1f
        )

        currentRotationText.text = currentRotation!!.stringify()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun repeatedlyRun(intervalMs: Long, func: () -> Unit)
    {
        var execute: () -> Unit = {  }
        execute = {
            handler.postDelayed({
                func()
                execute()
            }, intervalMs)
        }
        execute()
    }

    private fun a(num: Float): String = String.format("%.3f", num)

    private fun Matrix4f.stringify(): String = "x: ${a(this[0, 0])}, ${a(this[0, 1])}, ${a(this[0, 2])}\n" +
                                               "y: ${a(this[1, 0])}, ${a(this[1, 1])}, ${a(this[1, 2])}\n" +
                                               "z: ${a(this[2, 0])}, ${a(this[2, 1])}, ${a(this[2, 2])}\n"
}