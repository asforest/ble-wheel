package com.github.asforest.blew.activity

import android.annotation.SuppressLint
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Handler
import android.view.MotionEvent
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.github.asforest.blew.R
import com.github.asforest.blew.ble.impl.HIDGamepad
import org.joml.Math
import org.joml.Matrix4f
import org.joml.Quaternionf
import org.joml.Vector3f
import kotlin.math.abs

class DrivingActivity : AppCompatActivity(), SensorEventListener
{
    val handler: Handler by lazy { Handler(mainLooper) }
    val sensorManager: SensorManager by lazy { getSystemService(SENSOR_SERVICE) as SensorManager }
    val rotationSensor: Sensor by lazy { sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) }

    val currentRotationText: TextView by lazy { findViewById(R.id.current_rotation) }
    val referenceRotationText: TextView by lazy { findViewById(R.id.reference_rotation) }
    val primaryButton: Button by lazy { findViewById(R.id.button_center) }
    val functionalButtons by lazy { arrayOf<Button>(
        findViewById(R.id.gamepad_button1),
        findViewById(R.id.gamepad_button2),
        findViewById(R.id.gamepad_button3),
        findViewById(R.id.gamepad_button4),
        findViewById(R.id.gamepad_button5),
        findViewById(R.id.gamepad_button6),
        findViewById(R.id.gamepad_button7),
        findViewById(R.id.gamepad_button8),
    ) }

    var reportingEnabled = true // 为false时禁用所有旋转，也不向BLE报告陀螺仪数据
    var currentRotation: Matrix4f? = null
    var referenceRotation: Matrix4f? = null

    val hidGamepad: HIDGamepad get() = MainActivity._gamepad!!

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driving)

        sensorManager.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_GAME)

        // 监听按钮点击
        primaryButton.setOnClickListener {
            // 记录下参考值
            if (referenceRotation == null)
                referenceRotation = Matrix4f()
            referenceRotation!!.set(currentRotation)
            referenceRotationText.text = referenceRotation!!.stringify()

            // 复位数据
            accumulatedAngleZ = 0f
            accumulatedAngleX = 0f
            previousAngleZ = null
            previousAngleX = null
        }

        primaryButton.setOnLongClickListener {
            reportingEnabled = !reportingEnabled
            primaryButton.alpha = if (reportingEnabled) 1f else 0.5f
            true
        }

        for ((index, button) in functionalButtons.withIndex())
        {
            button.setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_DOWN)
                {
                    hidGamepad.press((index + 1).toUByte())
                    hidGamepad.sendReport()
                }

                if (event.action == MotionEvent.ACTION_UP)
                {
                    hidGamepad.release((index + 1).toUByte())
                    hidGamepad.sendReport()
                }

                true
            }
        }

        // 定时报告数据
        repeatedlyRun(100) { reportSensorData() }

        // 处理设备断开事件
        hidGamepad.onDeviceConnectionStateChangeEvent.once {
            if (!it)
                finish()
        }
    }

    override fun onDestroy()
    {
        super.onDestroy()
        sensorManager.unregisterListener(this)
    }

    var accumulatedAngleZ: Float = 0f // 方向盘当前累计旋转角度（可能超过360）
    var accumulatedAngleX: Float = 0f // 油门刹车轴当前累计旋转角度（可能超过360）
    private var previousAngleZ: Float? = null // 上一次的位置角度，用来计算角度delta
    private var previousAngleX: Float? = null // 上一次的位置角度，用来计算角度delta
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
        val currentAngleZ = Math.toDegrees(rotatedY.angleSigned(refY, refZ).toDouble()).toFloat()

        val rotateOnX = Quaternionf()
        currX.rotationTo(refX, rotateOnX)
        val rotatedZ = currZ.rotate(rotateOnX)
        val currentAngleX = Math.toDegrees(rotatedZ.angleSigned(refZ, refX).toDouble()).toFloat()

        var reported = false

        /**
         * 报告一次数据
         */
        fun reportOnce()
        {
            if (!reported)
                hidGamepad.sendReport()
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
        if (previousAngleZ != null)
        {
            // 计算方向盘实际朝向
            accumulatedAngleZ += angleDelta(currentAngleZ, previousAngleZ!!)

            // 方向盘的最大旋转角度（一半）
            val halfConstraint = 180f

            // 归一化（默认位置是在0.5）
            val normalized = (Math.clamp(-halfConstraint, halfConstraint, accumulatedAngleZ) + halfConstraint) / (halfConstraint * 2)

            // 转换成HID协议范围的值
            val hidValue = ((65534 * normalized).toInt() - 32768).toShort()

            // 上报
            hidGamepad.setSteering(hidValue)
            reportOnce()
        }
        previousAngleZ = currentAngleZ

        // 计算油门刹车实际角度
        if (previousAngleX != null)
        {
            // 计算方向盘实际朝向
            accumulatedAngleX += angleDelta(currentAngleX, previousAngleX!!)

            // 最大旋转角度（一半）
            val halfConstraint = 90f

            // 归一化（默认位置是在0.5）
            val normalized = (Math.clamp(-halfConstraint, halfConstraint, accumulatedAngleX) + halfConstraint) / (halfConstraint * 2)

            // 转换成HID协议范围的值
            val hidValue = ((65534 * normalized).toInt() - 32768).toShort()

            // 上报
            hidGamepad.setBrake(hidValue)
            hidGamepad.setAccelerator((65535 - hidValue).toShort())
            reportOnce()
        }
        previousAngleX = currentAngleX

        // 更新UI
        primaryButton.rotation = -currentAngleZ
        primaryButton.text = "Z: ${a(currentAngleZ)} / ${a(accumulatedAngleZ)}\nX: ${a(currentAngleX)} / ${a(accumulatedAngleX)}"
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