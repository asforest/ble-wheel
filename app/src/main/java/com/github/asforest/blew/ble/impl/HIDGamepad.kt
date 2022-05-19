package com.github.asforest.blew.ble.impl

import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import android.util.Log
import com.github.asforest.blew.ble.BLE
import com.github.asforest.blew.ble.HIDPeripheral
import com.github.asforest.blew.util.BinaryUtils.high8
import com.github.asforest.blew.util.BinaryUtils.low8
import com.github.asforest.blew.util.BinaryUtils.reversedHighLowByte
import kotlin.experimental.and
import kotlin.experimental.or


class HIDGamepad(context: Context, var config: GamepadConfiguration) : HIDPeripheral(context)
{
    var _buttons = ByteArray(16) // 8 bits x 16 --> 128 bits // 实际类型为 uint8_t
    var _specialButtons: Byte = 0 // 实际类型为 uint8_t
    var _x: Short = 0 // 实际类型为 int16_t
    var _y: Short = 0 // 实际类型为 int16_t
    var _z: Short = 0 // 实际类型为 int16_t
    var _rZ: Short = 0 // 实际类型为 int16_t
    var _rX: Short = 0 // 实际类型为 int16_t
    var _rY: Short = 0 // 实际类型为 int16_t
    var _slider1: Short = 0 // 实际类型为 int16_t
    var _slider2: Short = 0 // 实际类型为 int16_t
    var _rudder: Short = 0 // 实际类型为 int16_t
    var _throttle: Short = 0 // 实际类型为 int16_t
    var _accelerator: Short = 0 // 实际类型为 int16_t
    var _brake: Short = 0 // 实际类型为 int16_t
    var _steering: Short = 0 // 实际类型为 int16_t
    var _hat1: Short = 0 // 实际类型为 int16_t
    var _hat2: Short = 0 // 实际类型为 int16_t
    var _hat3: Short = 0 // 实际类型为 int16_t
    var _hat4: Short = 0 // 实际类型为 int16_t

    var buttonPaddingBits: Int
    var specialButtonPaddingBits: Int
    var numOfAxisBytes: Int
    var numOfSimulationBytes: Int
    var numOfButtonBytes: Int
    var numOfSpecialButtonBytes: Int

    val reportSize: Int
    val hidReportMap: ByteArray

    val inputGamepad: BluetoothGattCharacteristic

    init {
        resetButtons()

        buttonPaddingBits = 8 - (config.buttonCount.toInt() % 8)
        if (buttonPaddingBits == 8)
            buttonPaddingBits = 0

        specialButtonPaddingBits = 8 - (config.totalSpecialButtonCount % 8)
        if (specialButtonPaddingBits == 8)
            specialButtonPaddingBits = 0

        numOfAxisBytes = config.axisCount * 2
        numOfSimulationBytes = config.simulationCount * 2

        numOfButtonBytes = config.buttonCount.toInt() / 8
        if (buttonPaddingBits > 0)
            numOfButtonBytes += 1

        numOfSpecialButtonBytes = config.totalSpecialButtonCount / 8
        if (specialButtonPaddingBits > 0)
            numOfSpecialButtonBytes += 1

        reportSize = numOfButtonBytes + numOfSpecialButtonBytes + numOfAxisBytes + numOfSimulationBytes + config.hatSwitchCount.toInt()

        hidReportMap = buildReportMap()

        inputGamepad = hid.inputReport(BLE.REPORT_ID_GAMEPAD)

        val reversedPid = config.pid.reversedHighLowByte
        val reversedVid = config.vid.reversedHighLowByte
        hid.pnp(0x01, reversedVid, reversedPid, 0x110)

        hid.hidInfo(0x00, 0x01)

        Log.i("App", "HidReportMap: "+hidReportMap.joinToString("") { String.format("%02x", it) })

        hid.reportMap(hidReportMap)

        hid.startServices()
    }

    fun buildReportMap(): ByteArray
    {
        val report = ByteArray(150)
        var size = 0

        // USAGE_PAGE (Generic Desktop)
        report[size++] = 0x05
        report[size++] = 0x01

        // USAGE (Joystick - 0x04; Gamepad - 0x05; Multi-axis Controller - 0x08)
        report[size++] = 0x09
        report[size++] = config.controllerType.toByte()

        // COLLECTION (Application)
        report[size++] = 0xa1.toByte()
        report[size++] = 0x01

        // REPORT_ID (Default: 3)
        report[size++] = 0x85.toByte()
        report[size++] = config.hidReportId.toByte()

        if (config.buttonCount > 0)
        {
            // USAGE_PAGE (Button)
            report[size++] = 0x05
            report[size++] = 0x09

            // LOGICAL_MINIMUM (0)
            report[size++] = 0x15
            report[size++] = 0x00

            // LOGICAL_MAXIMUM (1)
            report[size++] = 0x25
            report[size++] = 0x01

            // REPORT_SIZE (1)
            report[size++] = 0x75
            report[size++] = 0x01

            // USAGE_MINIMUM (Button 1)
            report[size++] = 0x19
            report[size++] = 0x01

            // USAGE_MAXIMUM (Up to 128 buttons possible)
            report[size++] = 0x29
            report[size++] = config.buttonCount.low8

            // REPORT_COUNT (# of buttons)
            report[size++] = 0x95.toByte()
            report[size++] = config.buttonCount.low8

            // INPUT (Data,Var,Abs)
            report[size++] = 0x81.toByte()
            report[size++] = 0x02

            if (buttonPaddingBits > 0)
            {
                // REPORT_SIZE (1)
                report[size++] = 0x75
                report[size++] = 0x01

                // REPORT_COUNT (# of padding bits)
                report[size++] = 0x95.toByte()
                report[size++] = buttonPaddingBits.toByte()

                // INPUT (Const,Var,Abs)
                report[size++] = 0x81.toByte()
                report[size++] = 0x03

            } // Padding Bits Needed

        } // Buttons

        if (config.totalSpecialButtonCount > 0)
        {
            // LOGICAL_MINIMUM (0)
            report[size++] = 0x15
            report[size++] = 0x00

            // LOGICAL_MAXIMUM (1)
            report[size++] = 0x25
            report[size++] = 0x01

            // REPORT_SIZE (1)
            report[size++] = 0x75
            report[size++] = 0x01

            if (config.desktopSpecialButtonCount > 0)
            {

                // USAGE_PAGE (Generic Desktop)
                report[size++] = 0x05
                report[size++] = 0x01

                // REPORT_COUNT
                report[size++] = 0x95.toByte()
                report[size++] = config.desktopSpecialButtonCount

                if (config.getIncludeStart)
                {
                    // USAGE (Start)
                    report[size++] = 0x09
                    report[size++] = 0x3D
                }

                if (config.getIncludeSelect)
                {
                    // USAGE (Select)
                    report[size++] = 0x09
                    report[size++] = 0x3E
                }

                if (config.getIncludeMenu)
                {
                    // USAGE (App Menu)
                    report[size++] = 0x09
                    report[size++] = 0x86.toByte()
                }

                // INPUT (Data,Var,Abs)
                report[size++] = 0x81.toByte()
                report[size++] = 0x02
            }

            if (config.consumerSpecialButtonCount > 0)
            {

                // USAGE_PAGE (Consumer Page)
                report[size++] = 0x05
                report[size++] = 0x0C

                // REPORT_COUNT
                report[size++] = 0x95.toByte()
                report[size++] = config.consumerSpecialButtonCount

                if (config.getIncludeHome)
                {
                    // USAGE (Home)
                    report[size++] = 0x0A
                    report[size++] = 0x23
                    report[size++] = 0x02
                }

                if (config.getIncludeBack)
                {
                    // USAGE (Back)
                    report[size++] = 0x0A
                    report[size++] = 0x24
                    report[size++] = 0x02
                }

                if (config.getIncludeVolumeInc)
                {
                    // USAGE (Volume Increment)
                    report[size++] = 0x09
                    report[size++] = 0xE9.toByte()
                }

                if (config.getIncludeVolumeDec)
                {
                    // USAGE (Volume Decrement)
                    report[size++] = 0x09
                    report[size++] = 0xEA.toByte()
                }

                if (config.getIncludeVolumeMute)
                {
                    // USAGE (Mute)
                    report[size++] = 0x09
                    report[size++] = 0xE2.toByte()
                }

                // INPUT (Data,Var,Abs)
                report[size++] = 0x81.toByte()
                report[size++] = 0x02
            }

            if (specialButtonPaddingBits > 0)
            {

                // REPORT_SIZE (1)
                report[size++] = 0x75
                report[size++] = 0x01

                // REPORT_COUNT (# of padding bits)
                report[size++] = 0x95.toByte()
                report[size++] = specialButtonPaddingBits.toByte()

                // INPUT (Const,Var,Abs)
                report[size++] = 0x81.toByte()
                report[size++] = 0x03

            } // Padding Bits Needed

        } // Special Buttons

        if (config.axisCount > 0)
        {
            // USAGE_PAGE (Generic Desktop)
            report[size++] = 0x05
            report[size++] = 0x01

            // USAGE (Pointer)
            report[size++] = 0x09
            report[size++] = 0x01

            // LOGICAL_MINIMUM (-32767)
            report[size++] = 0x16
            report[size++] = 0x01
            report[size++] = 0x80.toByte()

            // LOGICAL_MAXIMUM (+32767)
            report[size++] = 0x26
            report[size++] = 0xFF.toByte()
            report[size++] = 0x7F

            // REPORT_SIZE (16)
            report[size++] = 0x75
            report[size++] = 0x10

            // REPORT_COUNT (config.getAxisCount())
            report[size++] = 0x95.toByte()
            report[size++] = config.axisCount

            // COLLECTION (Physical)
            report[size++] = 0xA1.toByte()
            report[size++] = 0x00

            if (config.getIncludeXAxis)
            {
                // USAGE (X)
                report[size++] = 0x09
                report[size++] = 0x30
            }

            if (config.getIncludeYAxis)
            {
                // USAGE (Y)
                report[size++] = 0x09
                report[size++] = 0x31
            }

            if (config.getIncludeZAxis)
            {
                // USAGE (Z)
                report[size++] = 0x09
                report[size++] = 0x32
            }

            if (config.getIncludeRzAxis)
            {
                // USAGE (Rz)
                report[size++] = 0x09
                report[size++] = 0x35
            }

            if (config.getIncludeRxAxis)
            {
                // USAGE (Rx)
                report[size++] = 0x09
                report[size++] = 0x33
            }

            if (config.getIncludeRyAxis)
            {
                // USAGE (Ry)
                report[size++] = 0x09
                report[size++] = 0x34
            }

            if (config.getIncludeSlider1)
            {
                // USAGE (Slider)
                report[size++] = 0x09
                report[size++] = 0x36
            }

            if (config.getIncludeSlider2)
            {
                // USAGE (Slider)
                report[size++] = 0x09
                report[size++] = 0x36
            }

            // INPUT (Data,Var,Abs)
            report[size++] = 0x81.toByte()
            report[size++] = 0x02

            // END_COLLECTION (Physical)
            report[size++] = 0xc0.toByte()

        } // X, Y, Z, Rx, Ry, and Rz Axis

        if (config.simulationCount > 0)
        {

            // USAGE_PAGE (Simulation Controls)
            report[size++] = 0x05
            report[size++] = 0x02

            // LOGICAL_MINIMUM (-32767)
            report[size++] = 0x16
            report[size++] = 0x01
            report[size++] = 0x80.toByte()

            // LOGICAL_MAXIMUM (+32767)
            report[size++] = 0x26
            report[size++] = 0xFF.toByte()
            report[size++] = 0x7F

            // REPORT_SIZE (16)
            report[size++] = 0x75
            report[size++] = 0x10

            // REPORT_COUNT (config.getSimulationCount())
            report[size++] = 0x95.toByte()
            report[size++] = config.simulationCount

            // COLLECTION (Physical)
            report[size++] = 0xA1.toByte()
            report[size++] = 0x00

            if (config.getIncludeRudder)
            {
                // USAGE (Rudder)
                report[size++] = 0x09
                report[size++] = 0xBA.toByte()
            }

            if (config.getIncludeThrottle)
            {
                // USAGE (Throttle)
                report[size++] = 0x09
                report[size++] = 0xBB.toByte()
            }

            if (config.getIncludeAccelerator)
            {
                // USAGE (Accelerator)
                report[size++] = 0x09
                report[size++] = 0xC4.toByte()
            }

            if (config.getIncludeBrake)
            {
                // USAGE (Brake)
                report[size++] = 0x09
                report[size++] = 0xC5.toByte()
            }

            if (config.getIncludeSteering)
            {
                // USAGE (Steering)
                report[size++] = 0x09
                report[size++] = 0xC8.toByte()
            }

            // INPUT (Data,Var,Abs)
            report[size++] = 0x81.toByte()
            report[size++] = 0x02

            // END_COLLECTION (Physical)
            report[size++] = 0xc0.toByte()

        } // Simulation Controls

        if (config.hatSwitchCount > 0)
        {

            // COLLECTION (Physical)
            report[size++] = 0xA1.toByte()
            report[size++] = 0x00

            // USAGE_PAGE (Generic Desktop)
            report[size++] = BLE.USAGE_PAGE(1)
            report[size++] = 0x01

            // USAGE (Hat Switch)
            for (currentHatIndex in 0 until config.hatSwitchCount)
            {
                report[size++] = BLE.USAGE(1)
                report[size++] = 0x39
            }

            // Logical Min (1)
            report[size++] = 0x15
            report[size++] = 0x01

            // Logical Max (8)
            report[size++] = 0x25
            report[size++] = 0x08

            // Physical Min (0)
            report[size++] = 0x35
            report[size++] = 0x00

            // Physical Max (315)
            report[size++] = 0x46
            report[size++] = 0x3B
            report[size++] = 0x01

            // Unit (SI Rot : Ang Pos)
            report[size++] = 0x65
            report[size++] = 0x12

            // Report Size (8)
            report[size++] = 0x75
            report[size++] = 0x08

            // Report Count (4)
            report[size++] = 0x95.toByte()
            report[size++] = config.hatSwitchCount

            // Input (Data, Variable, Absolute)
            report[size++] = 0x81.toByte()
            report[size++] = 0x42

            // END_COLLECTION (Physical)
            report[size++] = 0xc0.toByte()
        }

        // END_COLLECTION (Application)
        report[size++] = 0xc0.toByte()

        return report.copyOfRange(0, size)
    }

    fun setAxes(x: Short, y: Short, z: Short, rZ: Short, rX: Short, rY: Short, slider1: Short, slider2: Short)
    {
        _x = bound(x)
        _y = bound(_y)
        _z = bound(_z)
        _rZ = bound(_rZ)
        _rX = bound(_rX)
        _rY = bound(_rY)
        _slider1 = bound(_slider1)
        _slider2 = bound(_slider2)

        if (config.autoReport)
            sendReport()
    }

    fun setSimulationControls(rudder: Short, throttle: Short, accelerator: Short, brake: Short, steering: Short)
    {
        _rudder = bound(rudder)
        _throttle = bound(throttle)
        _accelerator = bound(accelerator)
        _brake = bound(brake)
        _steering = bound(steering)

        if (config.autoReport)
            sendReport()
    }

    fun setHats(hat1: Byte, hat2: Byte, hat3: Byte, hat4: Byte)
    {
        _hat1 = hat1.toShort()
        _hat2 = hat2.toShort()
        _hat3 = hat3.toShort()
        _hat4 = hat4.toShort()

        if (config.autoReport)
            sendReport()
    }

    fun setSliders(slider1: Short, slider2: Short) {
        _slider1 = bound(slider1)
        _slider2 = bound(slider2)

        if (config.autoReport)
            sendReport()
    }

    fun sendReport()
    {
        if (currentDevice == null)
            return

        var currentReportIndex = 0

        val m = ByteArray(reportSize)

        _buttons.copyInto(m, 0, 0)

        currentReportIndex += numOfButtonBytes

        if (config.totalSpecialButtonCount > 0)
        {
            m[currentReportIndex++] = _specialButtons
        }

        if (config.getIncludeXAxis)
        {
            m[currentReportIndex++] = _x.low8
            m[currentReportIndex++] = _x.high8
        }
        if (config.getIncludeYAxis)
        {
            m[currentReportIndex++] = _y.low8
            m[currentReportIndex++] = _y.high8
        }
        if (config.getIncludeZAxis)
        {
            m[currentReportIndex++] = _z.low8
            m[currentReportIndex++] = _z.high8
        }
        if (config.getIncludeRzAxis)
        {
            m[currentReportIndex++] = _rZ.low8
            m[currentReportIndex++] = _rZ.high8
        }
        if (config.getIncludeRxAxis)
        {
            m[currentReportIndex++] = _rX.low8
            m[currentReportIndex++] = _rX.high8
        }
        if (config.getIncludeRyAxis)
        {
            m[currentReportIndex++] = _rY.low8
            m[currentReportIndex++] = _rY.high8
        }

        if (config.getIncludeSlider1)
        {
            m[currentReportIndex++] = _slider1.low8
            m[currentReportIndex++] = _slider1.high8
        }
        if (config.getIncludeSlider2)
        {
            m[currentReportIndex++] = _slider2.low8
            m[currentReportIndex++] = _slider2.high8
        }

        if (config.getIncludeRudder)
        {
            m[currentReportIndex++] = _rudder.low8
            m[currentReportIndex++] = _rudder.high8
        }
        if (config.getIncludeThrottle)
        {
            m[currentReportIndex++] = _throttle.low8
            m[currentReportIndex++] = _throttle.high8
        }
        if (config.getIncludeAccelerator)
        {
            m[currentReportIndex++] = _accelerator.low8
            m[currentReportIndex++] = _accelerator.high8
        }
        if (config.getIncludeBrake)
        {
            m[currentReportIndex++] = _brake.low8
            m[currentReportIndex++] = _brake.high8
        }
        if (config.getIncludeSteering)
        {
            m[currentReportIndex++] = _steering.low8
            m[currentReportIndex++] = _steering.high8
        }

        if (config.hatSwitchCount > 0)
        {
            val hats = byteArrayOf(_hat1.toByte(), _hat2.toByte(), _hat3.toByte(), _hat4.toByte())

            for (currentHatIndex in config.hatSwitchCount - 1 downTo 0)
                m[currentReportIndex++] = hats[currentHatIndex]
        }

        inputGamepad.value = m
        gattServer.notifyCharacteristicChanged(currentDevice, inputGamepad, false)
    }


    fun press(button: UByte)
    {
        setButton(button, true)
    }

    fun release(button: UByte)
    {
        setButton(button, false)
    }

    fun setButton(button: UByte, press: Boolean)
    {
        val b = button.toByte()

        val index = (b - 1) / 8
        val bit = (b - 1) % 8
        val bitmask = 1 shl bit

        val result: Byte = if (press)
            _buttons[index] or bitmask.toByte()
        else
            _buttons[index] and (bitmask.inv().toByte())

        if (result != _buttons[index])
            _buttons[index] = result

        if (config.autoReport)
            sendReport()
    }

    fun specialButtonBitPosition(button: UByte): UByte
    {
        val b = button.toInt()

        if (b >= POSSIBLE_SPECIAL_BUTTONS)
            throw IndexOutOfBoundsException("Index out of range")

        var bit: UByte = 0u

        for (i in 0 until b)
            if (config.whichSpecialButtons[i])
                bit++

        return bit
    }

    fun setSpecialButton(button: UByte, press: Boolean)
    {
        val b: UByte = specialButtonBitPosition(button)
        val bit = b.toInt() % 8
        val bitmask = 1 shl bit

        val result: Byte = if (press)
            _specialButtons or bitmask.toByte()
        else
            _specialButtons and bitmask.inv().toByte()

        if (result != _specialButtons)
            _specialButtons = result

        if (config.autoReport)
            sendReport()
    }

    fun pressSpecialButton(button: UByte) { setSpecialButton(button, true) }

    fun releaseSpecialButton(button: UByte) { setSpecialButton(button, false) }

    fun pressStart() { pressSpecialButton(START_BUTTON.toUByte()) }

    fun releaseStart() { releaseSpecialButton(START_BUTTON.toUByte()) }

    fun pressSelect() { pressSpecialButton(SELECT_BUTTON.toUByte()) }

    fun releaseSelect() { releaseSpecialButton(SELECT_BUTTON.toUByte()) }

    fun pressMenu() { pressSpecialButton(MENU_BUTTON.toUByte()) }

    fun releaseMenu() { releaseSpecialButton(MENU_BUTTON.toUByte()) }

    fun pressHome() { pressSpecialButton(HOME_BUTTON.toUByte()) }

    fun releaseHome() { releaseSpecialButton(HOME_BUTTON.toUByte()) }

    fun pressBack() { pressSpecialButton(BACK_BUTTON.toUByte()) }

    fun releaseBack() { releaseSpecialButton(BACK_BUTTON.toUByte()) }

    fun pressVolumeInc() { pressSpecialButton(VOLUME_INC_BUTTON.toUByte()) }

    fun releaseVolumeInc() { releaseSpecialButton(VOLUME_INC_BUTTON.toUByte()) }

    fun pressVolumeDec() { pressSpecialButton(VOLUME_DEC_BUTTON.toUByte()) }

    fun releaseVolumeDec() { releaseSpecialButton(VOLUME_DEC_BUTTON.toUByte()) }

    fun pressVolumeMute() { pressSpecialButton(VOLUME_MUTE_BUTTON.toUByte()) }

    fun releaseVolumeMute() { releaseSpecialButton(VOLUME_MUTE_BUTTON.toUByte()) }

    fun setLeftThumb(x: Short, y: Short) {
        _x = bound(x)
        _y = bound(y)

        if (config.autoReport)
            sendReport()
    }

    fun setRightThumb(z: Short, rZ: Short) {
        _z = bound(z)
        _rZ = bound(rZ)

        if (config.autoReport)
            sendReport()
    }

    fun setLeftTrigger(rX: Short) {
        _rX = bound(rX)

        if (config.autoReport)
            sendReport()
    }

    fun setRightTrigger(rY: Short) {
        _rY = bound(rY)

        if (config.autoReport)
            sendReport()
    }

    fun setTriggers(rX: Short, rY: Short) {
        _rX = bound(rX)
        _rY = bound(rY)

        if (config.autoReport)
            sendReport()
    }

    fun setHat(hat: Byte) {
        _hat1 = hat.toShort()

        if (config.autoReport)
            sendReport()
    }

    fun setHat1(hat1: Byte) {
        _hat1 = hat1.toShort()

        if (config.autoReport)
            sendReport()
    }

    fun setHat2(hat2: Byte) {
        _hat2 = hat2.toShort()

        if (config.autoReport)
            sendReport()
    }

    fun setHat3(hat3: Byte) {
        _hat3 = hat3.toShort()

        if (config.autoReport)
            sendReport()
    }

    fun setHat4(hat4: Byte) {
        _hat4 = hat4.toShort()

        if (config.autoReport)
            sendReport()
    }

    fun setX(x: Short) {
        _x = bound(x)

        if (config.autoReport)
            sendReport()
    }

    fun setY(y: Short) {
        _y = bound(y)

        if (config.autoReport)
            sendReport()
    }

    fun setZ(z: Short) {
        _z = bound(z)

        if (config.autoReport)
            sendReport()
    }

    fun setRZ(rZ: Short) {
        _rZ = bound(rZ)

        if (config.autoReport)
            sendReport()
    }

    fun setRX(rX: Short) {
        _rX = bound(rX)

        if (config.autoReport)
            sendReport()
    }

    fun setRY(rY: Short) {
        _rY = bound(rY)

        if (config.autoReport)
            sendReport()
    }

    fun setSlider(slider: Short) {
        _slider1 = bound(slider)

        if (config.autoReport)
            sendReport()
    }

    fun setSlider1(slider1: Short) {
        _slider1 = bound(slider1)

        if (config.autoReport)
            sendReport()
    }

    fun setSlider2(slider2: Short) {

        _slider2 = bound(slider2)

        if (config.autoReport)
            sendReport()
    }

    fun setRudder(rudder: Short) {
        _rudder = bound(rudder)

        if (config.autoReport)
            sendReport()
    }

    fun setThrottle(throttle: Short) {
        _throttle = bound(throttle)

        if (config.autoReport)
            sendReport()
    }

    fun setAccelerator(accelerator: Short) {
        _accelerator = bound(accelerator)

        if (config.autoReport)
            sendReport()
    }

    fun setBrake(brake: Short) {
        _brake = bound(brake)

        if (config.autoReport)
            sendReport()
    }

    fun setSteering(steering: Short) {
        _steering = bound(steering)

        if (config.autoReport)
            sendReport()
    }

    fun isPressed(button: UByte): Boolean
    {
        val b = button.toInt()
        val index = (b - 1) / 8
        val bit = (b - 1) % 8
        val bitmask = (1 shl bit)

        return (bitmask and _buttons[index].toInt()) > 0
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun bound(value: Int): Short = bound(value.toShort())
    @Suppress("NOTHING_TO_INLINE")
    private inline fun bound(value: Short): Short = if (value == (-32768).toShort()) -32767 else value

    fun resetButtons() { _buttons = ByteArray(16) }

    class GamepadConfiguration
    {
        var controllerType: Byte = CONTROLLER_TYPE_GAMEPAD
        var autoReport: Boolean = true
        var hidReportId: Byte = BLE.REPORT_ID_GAMEPAD
        var buttonCount: Short = 16
        var hatSwitchCount: Byte = 1
        var whichSpecialButtons: BooleanArray = booleanArrayOf(true, true, false, false, false, false, false, false)
        var whichAxes: BooleanArray = booleanArrayOf(true, true, true, true, true, true, true, true)
        var whichSimulationControls: BooleanArray = booleanArrayOf(false, false, false, false, false)
        var vid: Short = (0xe502).toShort()
        var pid: Short = (0xbbab).toShort()

        val totalSpecialButtonCount: Byte get() = whichSpecialButtons.count { it }.toByte()
        val desktopSpecialButtonCount: Byte get() = whichSpecialButtons.copyOfRange(0, 4).count { it }.toByte()
        val consumerSpecialButtonCount: Byte get() = whichSpecialButtons.copyOfRange(4, 8).count { it }.toByte()
        val axisCount: Byte get() = whichAxes.count { it }.toByte()
        val simulationCount: Byte get() = whichSimulationControls.count { it }.toByte()

        val getIncludeStart: Boolean get() = whichSpecialButtons[START_BUTTON]
        val getIncludeSelect: Boolean get() = whichSpecialButtons[SELECT_BUTTON]
        val getIncludeMenu: Boolean get() = whichSpecialButtons[MENU_BUTTON]
        val getIncludeHome: Boolean get() = whichSpecialButtons[HOME_BUTTON]
        val getIncludeBack: Boolean get() = whichSpecialButtons[BACK_BUTTON]
        val getIncludeVolumeInc: Boolean get() = whichSpecialButtons[VOLUME_INC_BUTTON]
        val getIncludeVolumeDec: Boolean get() = whichSpecialButtons[VOLUME_DEC_BUTTON]
        val getIncludeVolumeMute: Boolean get() = whichSpecialButtons[VOLUME_MUTE_BUTTON]
        val getIncludeXAxis: Boolean get() = whichAxes[X_AXIS]
        val getIncludeYAxis: Boolean get() = whichAxes[Y_AXIS]
        val getIncludeZAxis: Boolean get() = whichAxes[Z_AXIS]
        val getIncludeRxAxis: Boolean get() = whichAxes[RX_AXIS]
        val getIncludeRyAxis: Boolean get() = whichAxes[RY_AXIS]
        val getIncludeRzAxis: Boolean get() = whichAxes[RZ_AXIS]
        val getIncludeSlider1: Boolean get() = whichAxes[SLIDER1]
        val getIncludeSlider2: Boolean get() = whichAxes[SLIDER2]
        val getIncludeRudder: Boolean get() = whichSimulationControls[RUDDER]
        val getIncludeThrottle: Boolean get() = whichSimulationControls[THROTTLE]
        val getIncludeAccelerator: Boolean get() = whichSimulationControls[ACCELERATOR]
        val getIncludeBrake: Boolean get() = whichSimulationControls[BRAKE]
        val getIncludeSteering: Boolean get() = whichSimulationControls[STEERING]
        fun setIncludeStart(value: Boolean) { whichSpecialButtons[START_BUTTON] = value }
        fun setIncludeSelect(value: Boolean) { whichSpecialButtons[SELECT_BUTTON] = value }
        fun setIncludeMenu(value: Boolean) { whichSpecialButtons[MENU_BUTTON] = value }
        fun setIncludeHome(value: Boolean) { whichSpecialButtons[HOME_BUTTON] = value }
        fun setIncludeBack(value: Boolean) { whichSpecialButtons[BACK_BUTTON] = value }
        fun setIncludeVolumeInc(value: Boolean) { whichSpecialButtons[VOLUME_INC_BUTTON] = value }
        fun setIncludeVolumeDec(value: Boolean) { whichSpecialButtons[VOLUME_DEC_BUTTON] = value }
        fun setIncludeVolumeMute(value: Boolean) { whichSpecialButtons[VOLUME_MUTE_BUTTON] = value }
        fun setIncludeXAxis(value: Boolean) { whichAxes[X_AXIS] = value }
        fun setIncludeYAxis(value: Boolean) { whichAxes[Y_AXIS] = value }
        fun setIncludeZAxis(value: Boolean) { whichAxes[Z_AXIS] = value }
        fun setIncludeRxAxis(value: Boolean) { whichAxes[RX_AXIS] = value }
        fun setIncludeRyAxis(value: Boolean) { whichAxes[RY_AXIS] = value }
        fun setIncludeRzAxis(value: Boolean) { whichAxes[RZ_AXIS] = value }
        fun setIncludeSlider1(value: Boolean) { whichAxes[SLIDER1] = value }
        fun setIncludeSlider2(value: Boolean) { whichAxes[SLIDER2] = value }
        fun setIncludeRudder(value: Boolean) { whichSimulationControls[RUDDER] = value }
        fun setIncludeThrottle(value: Boolean) { whichSimulationControls[THROTTLE] = value }
        fun setIncludeAccelerator(value: Boolean) { whichSimulationControls[ACCELERATOR] = value }
        fun setIncludeBrake(value: Boolean) { whichSimulationControls[BRAKE] = value }
        fun setIncludeSteering(value: Boolean) { whichSimulationControls[STEERING] = value }

        fun setWhichSpecialButtons(start: Boolean, select: Boolean, menu: Boolean, home: Boolean, back: Boolean, volumeInc: Boolean, volumeDec: Boolean, volumeMute: Boolean)
        {
            whichSpecialButtons[START_BUTTON] = start
            whichSpecialButtons[SELECT_BUTTON] = select
            whichSpecialButtons[MENU_BUTTON] = menu
            whichSpecialButtons[HOME_BUTTON] = home
            whichSpecialButtons[BACK_BUTTON] = back
            whichSpecialButtons[VOLUME_INC_BUTTON] = volumeInc
            whichSpecialButtons[VOLUME_DEC_BUTTON] = volumeDec
            whichSpecialButtons[VOLUME_MUTE_BUTTON] = volumeMute
        }

        fun setWhichAxes(xAxis: Boolean, yAxis: Boolean, zAxis: Boolean, rxAxis: Boolean, ryAxis: Boolean, rzAxis: Boolean, slider1: Boolean, slider2: Boolean)
        {
            whichAxes[X_AXIS] = xAxis
            whichAxes[Y_AXIS] = yAxis
            whichAxes[Z_AXIS] = zAxis
            whichAxes[RX_AXIS] = rxAxis
            whichAxes[RY_AXIS] = ryAxis
            whichAxes[RZ_AXIS] = rzAxis

            whichAxes[SLIDER1] = slider1
            whichAxes[SLIDER2] = slider2
        }

        fun setWhichSimulationControls(rudder: Boolean, throttle: Boolean, accelerator: Boolean, brake: Boolean, steering: Boolean)
        {
            whichSimulationControls[RUDDER] = rudder
            whichSimulationControls[THROTTLE] = throttle
            whichSimulationControls[ACCELERATOR] = accelerator
            whichSimulationControls[BRAKE] = brake
            whichSimulationControls[STEERING] = steering
        }
    }

    companion object {
        const val POSSIBLE_SPECIAL_BUTTONS = 8
        const val POSSIBLE_AXES = 8
        const val POSSIBLE_SIMULATION_CONTROLS = 5

        const val CONTROLLER_TYPE_JOYSTICK: Byte = 0x04
        const val CONTROLLER_TYPE_GAMEPAD: Byte = 0x05
        const val CONTROLLER_TYPE_MULTI_AXIS: Byte = 0x08

        const val BUTTON_1: Byte = 0x1
        const val BUTTON_2: Byte = 0x2
        const val BUTTON_3: Byte = 0x3
        const val BUTTON_4: Byte = 0x4
        const val BUTTON_5: Byte = 0x5
        const val BUTTON_6: Byte = 0x6
        const val BUTTON_7: Byte = 0x7
        const val BUTTON_8: Byte = 0x8

        const val BUTTON_9: Byte = 0x9
        const val BUTTON_10: Byte = 0xa
        const val BUTTON_11: Byte = 0xb
        const val BUTTON_12: Byte = 0xc
        const val BUTTON_13: Byte = 0xd
        const val BUTTON_14: Byte = 0xe
        const val BUTTON_15: Byte = 0xf
        const val BUTTON_16: Byte = 0x10

        const val BUTTON_17: Byte = 0x11
        const val BUTTON_18: Byte = 0x12
        const val BUTTON_19: Byte = 0x13
        const val BUTTON_20: Byte = 0x14
        const val BUTTON_21: Byte = 0x15
        const val BUTTON_22: Byte = 0x16
        const val BUTTON_23: Byte = 0x17
        const val BUTTON_24: Byte = 0x18

        const val BUTTON_25: Byte = 0x19
        const val BUTTON_26: Byte = 0x1a
        const val BUTTON_27: Byte = 0x1b
        const val BUTTON_28: Byte = 0x1c
        const val BUTTON_29: Byte = 0x1d
        const val BUTTON_30: Byte = 0x1e
        const val BUTTON_31: Byte = 0x1f
        const val BUTTON_32: Byte = 0x20

        const val BUTTON_33: Byte = 0x21
        const val BUTTON_34: Byte = 0x22
        const val BUTTON_35: Byte = 0x23
        const val BUTTON_36: Byte = 0x24
        const val BUTTON_37: Byte = 0x25
        const val BUTTON_38: Byte = 0x26
        const val BUTTON_39: Byte = 0x27
        const val BUTTON_40: Byte = 0x28

        const val BUTTON_41: Byte = 0x29
        const val BUTTON_42: Byte = 0x2a
        const val BUTTON_43: Byte = 0x2b
        const val BUTTON_44: Byte = 0x2c
        const val BUTTON_45: Byte = 0x2d
        const val BUTTON_46: Byte = 0x2e
        const val BUTTON_47: Byte = 0x2f
        const val BUTTON_48: Byte = 0x30

        const val BUTTON_49: Byte = 0x31
        const val BUTTON_50: Byte = 0x32
        const val BUTTON_51: Byte = 0x33
        const val BUTTON_52: Byte = 0x34
        const val BUTTON_53: Byte = 0x35
        const val BUTTON_54: Byte = 0x36
        const val BUTTON_55: Byte = 0x37
        const val BUTTON_56: Byte = 0x38

        const val BUTTON_57: Byte = 0x39
        const val BUTTON_58: Byte = 0x3a
        const val BUTTON_59: Byte = 0x3b
        const val BUTTON_60: Byte = 0x3c
        const val BUTTON_61: Byte = 0x3d
        const val BUTTON_62: Byte = 0x3e
        const val BUTTON_63: Byte = 0x3f
        const val BUTTON_64: Byte = 0x40

        const val BUTTON_65: Byte = 0x41
        const val BUTTON_66: Byte = 0x42
        const val BUTTON_67: Byte = 0x43
        const val BUTTON_68: Byte = 0x44
        const val BUTTON_69: Byte = 0x45
        const val BUTTON_70: Byte = 0x46
        const val BUTTON_71: Byte = 0x47
        const val BUTTON_72: Byte = 0x48

        const val BUTTON_73: Byte = 0x49
        const val BUTTON_74: Byte = 0x4a
        const val BUTTON_75: Byte = 0x4b
        const val BUTTON_76: Byte = 0x4c
        const val BUTTON_77: Byte = 0x4d
        const val BUTTON_78: Byte = 0x4e
        const val BUTTON_79: Byte = 0x4f
        const val BUTTON_80: Byte = 0x50

        const val BUTTON_81: Byte = 0x51
        const val BUTTON_82: Byte = 0x52
        const val BUTTON_83: Byte = 0x53
        const val BUTTON_84: Byte = 0x54
        const val BUTTON_85: Byte = 0x55
        const val BUTTON_86: Byte = 0x56
        const val BUTTON_87: Byte = 0x57
        const val BUTTON_88: Byte = 0x58

        const val BUTTON_89: Byte = 0x59
        const val BUTTON_90: Byte = 0x5a
        const val BUTTON_91: Byte = 0x5b
        const val BUTTON_92: Byte = 0x5c
        const val BUTTON_93: Byte = 0x5d
        const val BUTTON_94: Byte = 0x5e
        const val BUTTON_95: Byte = 0x5f
        const val BUTTON_96: Byte = 0x60

        const val BUTTON_97: Byte = 0x61
        const val BUTTON_98: Byte = 0x62
        const val BUTTON_99: Byte = 0x63
        const val BUTTON_100: Byte = 0x64
        const val BUTTON_101: Byte = 0x65
        const val BUTTON_102: Byte = 0x66
        const val BUTTON_103: Byte = 0x67
        const val BUTTON_104: Byte = 0x68

        const val BUTTON_105: Byte = 0x69
        const val BUTTON_106: Byte = 0x6a
        const val BUTTON_107: Byte = 0x6b
        const val BUTTON_108: Byte = 0x6c
        const val BUTTON_109: Byte = 0x6d
        const val BUTTON_110: Byte = 0x6e
        const val BUTTON_111: Byte = 0x6f
        const val BUTTON_112: Byte = 0x70

        const val BUTTON_113: Byte = 0x71
        const val BUTTON_114: Byte = 0x72
        const val BUTTON_115: Byte = 0x73
        const val BUTTON_116: Byte = 0x74
        const val BUTTON_117: Byte = 0x75
        const val BUTTON_118: Byte = 0x76
        const val BUTTON_119: Byte = 0x77
        const val BUTTON_120: Byte = 0x78

        const val BUTTON_121: Byte = 0x79
        const val BUTTON_122: Byte = 0x7a
        const val BUTTON_123: Byte = 0x7b
        const val BUTTON_124: Byte = 0x7c
        const val BUTTON_125: Byte = 0x7d
        const val BUTTON_126: Byte = 0x7e
        const val BUTTON_127: Byte = 0x7f
        const val BUTTON_128: Byte = (0x80).toByte()

        const val DPAD_CENTERED: Byte = 0
        const val DPAD_UP: Byte = 1
        const val DPAD_UP_RIGHT: Byte = 2
        const val DPAD_RIGHT: Byte = 3
        const val DPAD_DOWN_RIGHT: Byte = 4
        const val DPAD_DOWN: Byte = 5
        const val DPAD_DOWN_LEFT: Byte = 6
        const val DPAD_LEFT: Byte = 7
        const val DPAD_UP_LEFT: Byte = 8

        const val HAT_CENTERED: Byte = 0
        const val HAT_UP: Byte = 1
        const val HAT_UP_RIGHT: Byte = 2
        const val HAT_RIGHT: Byte = 3
        const val HAT_DOWN_RIGHT: Byte = 4
        const val HAT_DOWN: Byte = 5
        const val HAT_DOWN_LEFT: Byte = 6
        const val HAT_LEFT: Byte = 7
        const val HAT_UP_LEFT: Byte = 8

        const val X_AXIS = 0
        const val Y_AXIS = 1
        const val Z_AXIS = 2
        const val RX_AXIS = 3
        const val RY_AXIS = 4
        const val RZ_AXIS = 5
        const val SLIDER1 = 6
        const val SLIDER2 = 7

        const val RUDDER = 0
        const val THROTTLE = 1
        const val ACCELERATOR = 2
        const val BRAKE = 3
        const val STEERING = 4

        const val START_BUTTON = 0
        const val SELECT_BUTTON = 1
        const val MENU_BUTTON = 2
        const val HOME_BUTTON = 3
        const val BACK_BUTTON = 4
        const val VOLUME_INC_BUTTON = 5
        const val VOLUME_DEC_BUTTON = 6
        const val VOLUME_MUTE_BUTTON = 7
    }
}