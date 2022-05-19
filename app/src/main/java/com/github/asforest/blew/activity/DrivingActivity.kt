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
import org.joml.Matrix4d
import org.joml.Matrix4f

class DrivingActivity : AppCompatActivity(), SensorEventListener
{
    val handler: Handler by lazy { Handler(mainLooper) }
    val sensorManager: SensorManager by lazy { getSystemService(SENSOR_SERVICE) as SensorManager }
    val rotationSensor: Sensor by lazy { sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) }

    val currentRotationText: TextView by lazy { findViewById(R.id.current_rotation) }
    val referenceRotationText: TextView by lazy { findViewById(R.id.reference_rotation) }
    val primaryButton: Button by lazy { findViewById(R.id.button_center) }
    val fnButton1: Button by lazy { findViewById(R.id.gamepad_button1) }
    val fnButton2: Button by lazy { findViewById(R.id.gamepad_button2) }
    val fnButton3: Button by lazy { findViewById(R.id.gamepad_button3) }
    val fnButton4: Button by lazy { findViewById(R.id.gamepad_button4) }
    val fnButton5: Button by lazy { findViewById(R.id.gamepad_button5) }
    val fnButton6: Button by lazy { findViewById(R.id.gamepad_button6) }
    val fnButton7: Button by lazy { findViewById(R.id.gamepad_button7) }
    val fnButton8: Button by lazy { findViewById(R.id.gamepad_button8) }

    var rotatingEnabled = true
    var currentRotation = Matrix4d()
    var referenceRotation = Matrix4d()

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driving)

        sensorManager.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_GAME)

        // 监听按钮点击
        primaryButton.setOnClickListener {
            referenceRotation.set(currentRotation)
            referenceRotationText.text = referenceRotation.toString()
//            previousAngle = 10000f
//            steering = 0f
        }

        primaryButton.setOnLongClickListener {
            rotatingEnabled = !rotatingEnabled
            primaryButton.alpha = if (rotatingEnabled) 1f else 0.5f
            true
        }

        val allFnButtons = arrayOf(fnButton1, fnButton2, fnButton3, fnButton4,
                                    fnButton5, fnButton6, fnButton7, fnButton8)
        for (button in allFnButtons)
        {
            button.setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_DOWN)
                    buttoncmdqueue1.put(idx + 1)
                if (event.action == MotionEvent.ACTION_UP)
                    buttoncmdqueue1.put(-(idx + 1))
                true
            }
        }


    }

    private val temp = FloatArray(16)
    override fun onSensorChanged(event: SensorEvent)
    {
        if (event.sensor.type != Sensor.TYPE_ROTATION_VECTOR)
            return

        SensorManager.getRotationMatrixFromVector(temp, event.values)

        currentRotation.set(
            temp[0], temp[4], temp[8],   0f,
            temp[1], temp[5], temp[9],   0f,
            temp[2], temp[6], temp[10],  0f,
            0f,      0f,      0f,        1f
        )
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}