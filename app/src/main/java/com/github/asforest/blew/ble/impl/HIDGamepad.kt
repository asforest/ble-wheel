package com.github.asforest.blew.ble.impl

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattServer
import android.util.Log
import com.github.asforest.blew.ble.BLE
import com.github.asforest.blew.ble.HIDPeripheral
import com.github.asforest.blew.service.BLEGattServerService
import com.github.asforest.blew.util.BinaryUtils.high8
import com.github.asforest.blew.util.BinaryUtils.low8
import com.github.asforest.blew.util.BinaryUtils.reversedHighLowByte
import kotlin.experimental.and
import kotlin.experimental.or


class HIDGamepad(
    val config: GamepadConfiguration,
    val gattServer: BluetoothGattServer,
    val bleGattServerService: BLEGattServerService
) : HIDPeripheral(gattServer, bleGattServerService) {
    var _buttons = ByteArray(16) // 8 bits x 16 --> 128 bits // uint8_t
    var _specialButtons: Byte = 0 // uint8_t
    var _x: Short = 0 // int16_t
    var _y: Short = 0 // int16_t
    var _z: Short = 0 // int16_t
    var _rZ: Short = 0 // int16_t
    var _rX: Short = 0 // int16_t
    var _rY: Short = 0 // int16_t
    var _slider1: Short = 0 // int16_t
    var _slider2: Short = 0 // int16_t
    var _rudder: Short = 0 // int16_t
    var _throttle: Short = 0 // int16_t
    var _accelerator: Short = 0 // int16_t
    var _brake: Short = 0 // int16_t
    var _steering: Short = 0 // int16_t
    var _hat1: Short = 0 // int16_t
    var _hat2: Short = 0 // int16_t
    var _hat3: Short = 0 // int16_t
    var _hat4: Short = 0 // int16_t

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
            sendReport(false)
    }

    fun setSimulationControls(rudder: Short, throttle: Short, accelerator: Short, brake: Short, steering: Short)
    {
        _rudder = bound(rudder)
        _throttle = bound(throttle)
        _accelerator = bound(accelerator)
        _brake = bound(brake)
        _steering = bound(steering)

        if (config.autoReport)
            sendReport(false)
    }

    fun setHats(hat1: Byte, hat2: Byte, hat3: Byte, hat4: Byte)
    {
        _hat1 = hat1.toShort()
        _hat2 = hat2.toShort()
        _hat3 = hat3.toShort()
        _hat4 = hat4.toShort()

        if (config.autoReport)
            sendReport(false)
    }

    fun setSliders(slider1: Short, slider2: Short) {
        _slider1 = bound(slider1)
        _slider2 = bound(slider2)

        if (config.autoReport)
            sendReport(false)
    }

    fun sendReport(confirm: Boolean)
    {
        if (bleGattServerService.currentDevice == null)
            return

        var currentReportIndex = 0

        val m = ByteArray(reportSize)

        _buttons.copyInto(m, 0, 0)

        currentReportIndex += numOfButtonBytes

        if (config.totalSpecialButtonCount > 0)
        {
            m[currentReportIndex++] = _specialButtons.toByte()
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
        gattServer.notifyCharacteristicChanged(bleGattServerService.currentDevice, inputGamepad, confirm)
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
        val b = button.toInt()

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
            sendReport(true)
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
            sendReport(true)
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
            sendReport(false)
    }

    fun setRightThumb(z: Short, rZ: Short) {
        _z = bound(z)
        _rZ = bound(rZ)

        if (config.autoReport)
            sendReport(false)
    }

    fun setLeftTrigger(rX: Short) {
        _rX = bound(rX)

        if (config.autoReport)
            sendReport(false)
    }

    fun setRightTrigger(rY: Short) {
        _rY = bound(rY)

        if (config.autoReport)
            sendReport(false)
    }

    fun setTriggers(rX: Short, rY: Short) {
        _rX = bound(rX)
        _rY = bound(rY)

        if (config.autoReport)
            sendReport(false)
    }

    fun setHat(hat: Byte) {
        _hat1 = hat.toShort()

        if (config.autoReport)
            sendReport(false)
    }

    fun setHat1(hat1: Byte) {
        _hat1 = hat1.toShort()

        if (config.autoReport)
            sendReport(false)
    }

    fun setHat2(hat2: Byte) {
        _hat2 = hat2.toShort()

        if (config.autoReport)
            sendReport(false)
    }

    fun setHat3(hat3: Byte) {
        _hat3 = hat3.toShort()

        if (config.autoReport)
            sendReport(false)
    }

    fun setHat4(hat4: Byte) {
        _hat4 = hat4.toShort()

        if (config.autoReport)
            sendReport(false)
    }

    fun setX(x: Short) {
        _x = bound(x)

        if (config.autoReport)
            sendReport(false)
    }

    fun setY(y: Short) {
        _y = bound(y)

        if (config.autoReport)
            sendReport(false)
    }

    fun setZ(z: Short) {
        _z = bound(z)

        if (config.autoReport)
            sendReport(false)
    }

    fun setRZ(rZ: Short) {
        _rZ = bound(rZ)

        if (config.autoReport)
            sendReport(false)
    }

    fun setRX(rX: Short) {
        _rX = bound(rX)

        if (config.autoReport)
            sendReport(false)
    }

    fun setRY(rY: Short) {
        _rY = bound(rY)

        if (config.autoReport)
            sendReport(false)
    }

    fun setSlider(slider: Short) {
        _slider1 = bound(slider)

        if (config.autoReport)
            sendReport(false)
    }

    fun setSlider1(slider1: Short) {
        _slider1 = bound(slider1)

        if (config.autoReport)
            sendReport(false)
    }

    fun setSlider2(slider2: Short) {

        _slider2 = bound(slider2)

        if (config.autoReport)
            sendReport(false)
    }

    fun setRudder(rudder: Short) {
        _rudder = bound(rudder)

        if (config.autoReport)
            sendReport(false)
    }

    fun setThrottle(throttle: Short) {
        _throttle = bound(throttle)

        if (config.autoReport)
            sendReport(false)
    }

    fun setAccelerator(accelerator: Short) {
        _accelerator = bound(accelerator)

        if (config.autoReport)
            sendReport(false)
    }

    fun setBrake(brake: Short) {
        _brake = bound(brake)

        if (config.autoReport)
            sendReport(false)
    }

    fun setSteering(steering: Short) {
        _steering = bound(steering)

        if (config.autoReport)
            sendReport(false)
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
        var controllerType: UByte = CONTROLLER_TYPE_GAMEPAD
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
        val POSSIBLE_SPECIAL_BUTTONS = 8
        val POSSIBLE_AXES = 8
        val POSSIBLE_SIMULATION_CONTROLS = 5

        val CONTROLLER_TYPE_JOYSTICK: UByte = 0x04.toUByte()
        val CONTROLLER_TYPE_GAMEPAD: UByte = 0x05.toUByte()
        val CONTROLLER_TYPE_MULTI_AXIS: UByte = 0x08.toUByte()

        val BUTTON_1: UByte = 0x1.toUByte()
        val BUTTON_2: UByte = 0x2.toUByte()
        val BUTTON_3: UByte = 0x3.toUByte()
        val BUTTON_4: UByte = 0x4.toUByte()
        val BUTTON_5: UByte = 0x5.toUByte()
        val BUTTON_6: UByte = 0x6.toUByte()
        val BUTTON_7: UByte = 0x7.toUByte()
        val BUTTON_8: UByte = 0x8.toUByte()

        val BUTTON_9: UByte = 0x9.toUByte()
        val BUTTON_10: UByte = 0xa.toUByte()
        val BUTTON_11: UByte = 0xb.toUByte()
        val BUTTON_12: UByte = 0xc.toUByte()
        val BUTTON_13: UByte = 0xd.toUByte()
        val BUTTON_14: UByte = 0xe.toUByte()
        val BUTTON_15: UByte = 0xf.toUByte()
        val BUTTON_16: UByte = 0x10.toUByte()

        val BUTTON_17: UByte = 0x11.toUByte()
        val BUTTON_18: UByte = 0x12.toUByte()
        val BUTTON_19: UByte = 0x13.toUByte()
        val BUTTON_20: UByte = 0x14.toUByte()
        val BUTTON_21: UByte = 0x15.toUByte()
        val BUTTON_22: UByte = 0x16.toUByte()
        val BUTTON_23: UByte = 0x17.toUByte()
        val BUTTON_24: UByte = 0x18.toUByte()

        val BUTTON_25: UByte = 0x19.toUByte()
        val BUTTON_26: UByte = 0x1a.toUByte()
        val BUTTON_27: UByte = 0x1b.toUByte()
        val BUTTON_28: UByte = 0x1c.toUByte()
        val BUTTON_29: UByte = 0x1d.toUByte()
        val BUTTON_30: UByte = 0x1e.toUByte()
        val BUTTON_31: UByte = 0x1f.toUByte()
        val BUTTON_32: UByte = 0x20.toUByte()

        val BUTTON_33: UByte = 0x21.toUByte()
        val BUTTON_34: UByte = 0x22.toUByte()
        val BUTTON_35: UByte = 0x23.toUByte()
        val BUTTON_36: UByte = 0x24.toUByte()
        val BUTTON_37: UByte = 0x25.toUByte()
        val BUTTON_38: UByte = 0x26.toUByte()
        val BUTTON_39: UByte = 0x27.toUByte()
        val BUTTON_40: UByte = 0x28.toUByte()

        val BUTTON_41: UByte = 0x29.toUByte()
        val BUTTON_42: UByte = 0x2a.toUByte()
        val BUTTON_43: UByte = 0x2b.toUByte()
        val BUTTON_44: UByte = 0x2c.toUByte()
        val BUTTON_45: UByte = 0x2d.toUByte()
        val BUTTON_46: UByte = 0x2e.toUByte()
        val BUTTON_47: UByte = 0x2f.toUByte()
        val BUTTON_48: UByte = 0x30.toUByte()

        val BUTTON_49: UByte = 0x31.toUByte()
        val BUTTON_50: UByte = 0x32.toUByte()
        val BUTTON_51: UByte = 0x33.toUByte()
        val BUTTON_52: UByte = 0x34.toUByte()
        val BUTTON_53: UByte = 0x35.toUByte()
        val BUTTON_54: UByte = 0x36.toUByte()
        val BUTTON_55: UByte = 0x37.toUByte()
        val BUTTON_56: UByte = 0x38.toUByte()

        val BUTTON_57: UByte = 0x39.toUByte()
        val BUTTON_58: UByte = 0x3a.toUByte()
        val BUTTON_59: UByte = 0x3b.toUByte()
        val BUTTON_60: UByte = 0x3c.toUByte()
        val BUTTON_61: UByte = 0x3d.toUByte()
        val BUTTON_62: UByte = 0x3e.toUByte()
        val BUTTON_63: UByte = 0x3f.toUByte()
        val BUTTON_64: UByte = 0x40.toUByte()

        val BUTTON_65: UByte = 0x41.toUByte()
        val BUTTON_66: UByte = 0x42.toUByte()
        val BUTTON_67: UByte = 0x43.toUByte()
        val BUTTON_68: UByte = 0x44.toUByte()
        val BUTTON_69: UByte = 0x45.toUByte()
        val BUTTON_70: UByte = 0x46.toUByte()
        val BUTTON_71: UByte = 0x47.toUByte()
        val BUTTON_72: UByte = 0x48.toUByte()

        val BUTTON_73: UByte = 0x49.toUByte()
        val BUTTON_74: UByte = 0x4a.toUByte()
        val BUTTON_75: UByte = 0x4b.toUByte()
        val BUTTON_76: UByte = 0x4c.toUByte()
        val BUTTON_77: UByte = 0x4d.toUByte()
        val BUTTON_78: UByte = 0x4e.toUByte()
        val BUTTON_79: UByte = 0x4f.toUByte()
        val BUTTON_80: UByte = 0x50.toUByte()

        val BUTTON_81: UByte = 0x51.toUByte()
        val BUTTON_82: UByte = 0x52.toUByte()
        val BUTTON_83: UByte = 0x53.toUByte()
        val BUTTON_84: UByte = 0x54.toUByte()
        val BUTTON_85: UByte = 0x55.toUByte()
        val BUTTON_86: UByte = 0x56.toUByte()
        val BUTTON_87: UByte = 0x57.toUByte()
        val BUTTON_88: UByte = 0x58.toUByte()

        val BUTTON_89: UByte = 0x59.toUByte()
        val BUTTON_90: UByte = 0x5a.toUByte()
        val BUTTON_91: UByte = 0x5b.toUByte()
        val BUTTON_92: UByte = 0x5c.toUByte()
        val BUTTON_93: UByte = 0x5d.toUByte()
        val BUTTON_94: UByte = 0x5e.toUByte()
        val BUTTON_95: UByte = 0x5f.toUByte()
        val BUTTON_96: UByte = 0x60.toUByte()

        val BUTTON_97: UByte = 0x61.toUByte()
        val BUTTON_98: UByte = 0x62.toUByte()
        val BUTTON_99: UByte = 0x63.toUByte()
        val BUTTON_100: UByte = 0x64.toUByte()
        val BUTTON_101: UByte = 0x65.toUByte()
        val BUTTON_102: UByte = 0x66.toUByte()
        val BUTTON_103: UByte = 0x67.toUByte()
        val BUTTON_104: UByte = 0x68.toUByte()

        val BUTTON_105: UByte = 0x69.toUByte()
        val BUTTON_106: UByte = 0x6a.toUByte()
        val BUTTON_107: UByte = 0x6b.toUByte()
        val BUTTON_108: UByte = 0x6c.toUByte()
        val BUTTON_109: UByte = 0x6d.toUByte()
        val BUTTON_110: UByte = 0x6e.toUByte()
        val BUTTON_111: UByte = 0x6f.toUByte()
        val BUTTON_112: UByte = 0x70.toUByte()

        val BUTTON_113: UByte = 0x71.toUByte()
        val BUTTON_114: UByte = 0x72.toUByte()
        val BUTTON_115: UByte = 0x73.toUByte()
        val BUTTON_116: UByte = 0x74.toUByte()
        val BUTTON_117: UByte = 0x75.toUByte()
        val BUTTON_118: UByte = 0x76.toUByte()
        val BUTTON_119: UByte = 0x77.toUByte()
        val BUTTON_120: UByte = 0x78.toUByte()

        val BUTTON_121: UByte = 0x79.toUByte()
        val BUTTON_122: UByte = 0x7a.toUByte()
        val BUTTON_123: UByte = 0x7b.toUByte()
        val BUTTON_124: UByte = 0x7c.toUByte()
        val BUTTON_125: UByte = 0x7d.toUByte()
        val BUTTON_126: UByte = 0x7e.toUByte()
        val BUTTON_127: UByte = 0x7f.toUByte()
        val BUTTON_128: UByte = 0x80.toUByte()

        val DPAD_CENTERED: UByte = 0.toUByte()
        val DPAD_UP: UByte = 1.toUByte()
        val DPAD_UP_RIGHT: UByte = 2.toUByte()
        val DPAD_RIGHT: UByte = 3.toUByte()
        val DPAD_DOWN_RIGHT: UByte = 4.toUByte()
        val DPAD_DOWN: UByte = 5.toUByte()
        val DPAD_DOWN_LEFT: UByte = 6.toUByte()
        val DPAD_LEFT: UByte = 7.toUByte()
        val DPAD_UP_LEFT: UByte = 8.toUByte()

        val HAT_CENTERED: UByte = 0.toUByte()
        val HAT_UP: UByte = 1.toUByte()
        val HAT_UP_RIGHT: UByte = 2.toUByte()
        val HAT_RIGHT: UByte = 3.toUByte()
        val HAT_DOWN_RIGHT: UByte = 4.toUByte()
        val HAT_DOWN: UByte = 5.toUByte()
        val HAT_DOWN_LEFT: UByte = 6.toUByte()
        val HAT_LEFT: UByte = 7.toUByte()
        val HAT_UP_LEFT: UByte = 8.toUByte()

        val X_AXIS = 0
        val Y_AXIS = 1
        val Z_AXIS = 2
        val RX_AXIS = 3
        val RY_AXIS = 4
        val RZ_AXIS = 5
        val SLIDER1 = 6
        val SLIDER2 = 7

        val RUDDER = 0
        val THROTTLE = 1
        val ACCELERATOR = 2
        val BRAKE = 3
        val STEERING = 4

        val START_BUTTON = 0
        val SELECT_BUTTON = 1
        val MENU_BUTTON = 2
        val HOME_BUTTON = 3
        val BACK_BUTTON = 4
        val VOLUME_INC_BUTTON = 5
        val VOLUME_DEC_BUTTON = 6
        val VOLUME_MUTE_BUTTON = 7
    }
}