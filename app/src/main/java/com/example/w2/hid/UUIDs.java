package com.example.w2.hid;

import java.util.UUID;

public class UUIDs extends HidBase
{
    /**
     * Device Information Service
     */
    public static final UUID SERVICE_DEVICE_INFORMATION_0x180A = From(0x180A);
    public static final UUID CHARACTERISTIC_MANUFACTURER_NAME_0x2A29 = From(0x2A29);
    public static final UUID CHARACTERISTIC_MODEL_NUMBER_0x2A24 = From(0x2A24);
    public static final UUID CHARACTERISTIC_SERIAL_NUMBER_0x2A25 = From(0x2A25);

    /**
     * Battery Service
     */
    public static final UUID SERVICE_BATTERY_0x180F = From(0x180F);
    public static final UUID CHARACTERISTIC_BATTERY_LEVEL_0x2A19 = From(0x2A19);

    /**
     * HID Service
     */
    public static final UUID SERVICE_BLE_HID_0x1812 = From(0x1812);
    public static final UUID CHARACTERISTIC_HID_INFORMATION_0x2A4A = From(0x2A4A);
    public static final UUID CHARACTERISTIC_HID_REPORT_MAP_0x2A4B = From(0x2A4B);
    public static final UUID CHARACTERISTIC_HID_CONTROL_POINT_0x2A4C = From(0x2A4C);
    public static final UUID CHARACTERISTIC_HID_REPORT_0x2A4D = From(0x2A4D);
    public static final UUID CHARACTERISTIC_HID_PROTOCOL_MODE_0x2A4E = From(0x2A4E);

    /**
     * Gatt Characteristic Descriptor
     */
    public static final UUID DESCRIPTOR_REPORT_REFERENCE_0x2908 = From(0x2908);
    public static final UUID DESCRIPTOR_CLIENT_CHARACTERISTIC_CONFIGURATION_0x2902 = From(0x2902);
    public static final byte[] RESPONSE_HID_INFORMATION = {0x11, 0x01, 0x00, 0x03};

    public static final byte[] CLEAN_KEY_REPORT = new byte[] {0, 0, 0, 0, 0, 0, 0, 0};

    static UUID From(int value)
    {
        return UUID.fromString("0000" + String.format("%04X", value & 0xffff) + "-0000-1000-8000-00805F9B34FB");
    }

    static byte[] pnp(byte sig, short vid, short pid, short version)
    {
        return new byte[]{ sig, (byte) (vid >> 8), (byte) vid, (byte) (pid >> 8), (byte) pid, (byte) (version >> 8), (byte) version };
    }

    // Report IDs:
    static byte KEYBOARD_ID = 0x01;
    static byte MEDIA_KEYS_ID = 0x02;

    public static final byte[] HID_RM = {
            USAGE_PAGE(1),      0x01,          // USAGE_PAGE (Generic Desktop Ctrls)
            USAGE(1),           0x06,          // USAGE (Keyboard)
            COLLECTION(1),      0x01,          // COLLECTION (Application)
            // ------------------------------------------------- Keyboard
            REPORT_ID(1),       KEYBOARD_ID,   //   REPORT_ID (1)
            USAGE_PAGE(1),      0x07,          //   USAGE_PAGE (Kbrd/Keypad)
            USAGE_MINIMUM(1),   (byte) 0xE0,          //   USAGE_MINIMUM (0xE0)
            USAGE_MAXIMUM(1),   (byte) 0xE7,          //   USAGE_MAXIMUM (0xE7)
            LOGICAL_MINIMUM(1), 0x00,          //   LOGICAL_MINIMUM (0)
            LOGICAL_MAXIMUM(1), 0x01,          //   Logical Maximum (1)
            REPORT_SIZE(1),     0x01,          //   REPORT_SIZE (1)
            REPORT_COUNT(1),    0x08,          //   REPORT_COUNT (8)
            INPUT(1),        0x02,          //   INPUT (Data,Var,Abs,No Wrap,Linear,Preferred State,No Null Position)
            REPORT_COUNT(1),    0x01,          //   REPORT_COUNT (1) ; 1 byte (Reserved)
            REPORT_SIZE(1),     0x08,          //   REPORT_SIZE (8)
            INPUT(1),        0x01,          //   INPUT (Const,Array,Abs,No Wrap,Linear,Preferred State,No Null Position)
            REPORT_COUNT(1),    0x05,          //   REPORT_COUNT (5) ; 5 bits (Num lock, Caps lock, Scroll lock, Compose, Kana)
            REPORT_SIZE(1),     0x01,          //   REPORT_SIZE (1)
            USAGE_PAGE(1),      0x08,          //   USAGE_PAGE (LEDs)
            USAGE_MINIMUM(1),   0x01,          //   USAGE_MINIMUM (0x01) ; Num Lock
            USAGE_MAXIMUM(1),   0x05,          //   USAGE_MAXIMUM (0x05) ; Kana
            OUTPUT(1),       0x02,          //   OUTPUT (Data,Var,Abs,No Wrap,Linear,Preferred State,No Null Position,Non-volatile)
            REPORT_COUNT(1),    0x01,          //   REPORT_COUNT (1) ; 3 bits (Padding)
            REPORT_SIZE(1),     0x03,          //   REPORT_SIZE (3)
            OUTPUT(1),       0x01,          //   OUTPUT (Const,Array,Abs,No Wrap,Linear,Preferred State,No Null Position,Non-volatile)
            REPORT_COUNT(1),    0x06,          //   REPORT_COUNT (6) ; 6 bytes (Keys)
            REPORT_SIZE(1),     0x08,          //   REPORT_SIZE(8)
            LOGICAL_MINIMUM(1), 0x00,          //   LOGICAL_MINIMUM(0)
            LOGICAL_MAXIMUM(1), 0x65,          //   LOGICAL_MAXIMUM(0x65) ; 101 keys
            USAGE_PAGE(1),      0x07,          //   USAGE_PAGE (Kbrd/Keypad)
            USAGE_MINIMUM(1),   0x00,          //   USAGE_MINIMUM (0)
            USAGE_MAXIMUM(1),   0x65,          //   USAGE_MAXIMUM (0x65)
            INPUT(1),        0x00,          //   INPUT (Data,Array,Abs,No Wrap,Linear,Preferred State,No Null Position)
            END_COLLECTION(0),                 // END_COLLECTION
            // ------------------------------------------------- Media Keys
//            USAGE_PAGE(1),      0x0C,          // USAGE_PAGE (Consumer)
//            USAGE(1),           0x01,          // USAGE (Consumer Control)
//            COLLECTION(1),      0x01,          // COLLECTION (Application)
//            REPORT_ID(1),       MEDIA_KEYS_ID, //   REPORT_ID (3)
//            USAGE_PAGE(1),      0x0C,          //   USAGE_PAGE (Consumer)
//            LOGICAL_MINIMUM(1), 0x00,          //   LOGICAL_MINIMUM (0)
//            LOGICAL_MAXIMUM(1), 0x01,          //   LOGICAL_MAXIMUM (1)
//            REPORT_SIZE(1),     0x01,          //   REPORT_SIZE (1)
//            REPORT_COUNT(1),    0x10,          //   REPORT_COUNT (16)
//            USAGE(1),           (byte) 0xB5,          //   USAGE (Scan Next Track)     ; bit 0: 1
//            USAGE(1),           (byte) 0xB6,          //   USAGE (Scan Previous Track) ; bit 1: 2
//            USAGE(1),           (byte) 0xB7,          //   USAGE (Stop)                ; bit 2: 4
//            USAGE(1),           (byte) 0xCD,          //   USAGE (Play/Pause)          ; bit 3: 8
//            USAGE(1),           (byte) 0xE2,          //   USAGE (Mute)                ; bit 4: 16
//            USAGE(1),           (byte) 0xE9,          //   USAGE (Volume Increment)    ; bit 5: 32
//            USAGE(1),           (byte) 0xEA,          //   USAGE (Volume Decrement)    ; bit 6: 64
//            USAGE(2),           0x23, 0x02,    //   Usage (WWW Home)            ; bit 7: 128
//            USAGE(2),           (byte) 0x94, 0x01,    //   Usage (My Computer) ; bit 0: 1
//            USAGE(2),           (byte) 0x92, 0x01,    //   Usage (Calculator)  ; bit 1: 2
//            USAGE(2),           0x2A, 0x02,    //   Usage (WWW fav)     ; bit 2: 4
//            USAGE(2),           0x21, 0x02,    //   Usage (WWW search)  ; bit 3: 8
//            USAGE(2),           0x26, 0x02,    //   Usage (WWW stop)    ; bit 4: 16
//            USAGE(2),           0x24, 0x02,    //   Usage (WWW back)    ; bit 5: 32
//            USAGE(2),           (byte) 0x83, 0x01,    //   Usage (Media sel)   ; bit 6: 64
//            USAGE(2),           (byte) 0x8A, 0x01,    //   Usage (Mail)        ; bit 7: 128
//            INPUT(1),        0x02,          //   INPUT (Data,Var,Abs,No Wrap,Linear,Preferred State,No Null Position)
//            END_COLLECTION(0)                  // END_COLLECTION
    };

    public static final byte[] REPORT_MAP_KEYBOARD = {
        USAGE_PAGE(1),      0x01,       // Generic Desktop Ctrls
        USAGE(1),           0x06,       // Keyboard
        COLLECTION(1),      0x01,       // Application
        USAGE_PAGE(1),      0x07,       //   Kbrd/Keypad
        USAGE_MINIMUM(1), (byte) 0xE0,
        USAGE_MAXIMUM(1), (byte) 0xE7,
        LOGICAL_MINIMUM(1), 0x00,
        LOGICAL_MAXIMUM(1), 0x01,
        REPORT_SIZE(1),     0x01,       //   1 byte (Modifier)
        REPORT_COUNT(1),    0x08,
        INPUT(1),           0x02,       //   Data,Var,Abs,No Wrap,Linear,Preferred State,No Null Position
        REPORT_COUNT(1),    0x01,       //   1 byte (Reserved)
        REPORT_SIZE(1),     0x08,
        INPUT(1),           0x01,       //   Const,Array,Abs,No Wrap,Linear,Preferred State,No Null Position
        REPORT_COUNT(1),    0x05,       //   5 bits (Num lock, Caps lock, Scroll lock, Compose, Kana)
        REPORT_SIZE(1),     0x01,
        USAGE_PAGE(1),      0x08,       //   LEDs
        USAGE_MINIMUM(1),   0x01,       //   Num Lock
        USAGE_MAXIMUM(1),   0x05,       //   Kana
        OUTPUT(1),          0x02,       //   Data,Var,Abs,No Wrap,Linear,Preferred State,No Null Position,Non-volatile
        REPORT_COUNT(1),    0x01,       //   3 bits (Padding)
        REPORT_SIZE(1),     0x03,
        OUTPUT(1),          0x01,       //   Const,Array,Abs,No Wrap,Linear,Preferred State,No Null Position,Non-volatile
        REPORT_COUNT(1),    0x06,       //   6 bytes (Keys)
        REPORT_SIZE(1),     0x08,
        LOGICAL_MINIMUM(1), 0x00,
        LOGICAL_MAXIMUM(1), 0x65,       //   101 keys
        USAGE_PAGE(1),      0x07,       //   Kbrd/Keypad
        USAGE_MINIMUM(1),   0x00,
        USAGE_MAXIMUM(1),   0x65,
        INPUT(1),           0x00,       //   Data,Array,Abs,No Wrap,Linear,Preferred State,No Null Position
        END_COLLECTION(0),
    };

    public static final byte[] KEYBOARD_HID_DESCRIPTOR = new byte[] {
            0x05, 0x01,         // Usage Page (Generic Desktop)
            0x09, 0x06,         // Usage (Keyboard)
            (byte) 0xA1, 0x01,         // Collection (Application)
            0x05, 0x08,         //   Usage Page(LEDs)
            0x19, 0x01,         //   Usage Minimum (1)
            0x29, 0x03,         //   Usage Maxmum (3)
            0x15, 0x00,         //   Logical Minimum (0)
            0x25, 0x01,         //   Logical Maximum (1)
            0x75, 0x01,         //   Report Size (1)
            (byte) 0x95, 0x03,         //   Report Count (3)
            (byte) 0x91, 0x02,         //   Output(Absolute, Variable, Data) 1 bit x3
            (byte) 0x95, 0x05,         //   Report Count (5)
            (byte) 0x91, 0x01,         //   Output(Absolute, Array, Constant) 1 bit x5
            0x05, 0x07,         //   Usage Page(Keyboard)
            0x19, (byte) 0xE0,         //   Usage Minimum(224)
            0x29, (byte) 0xE7,         //   Usage Maxmum(231)
            (byte) 0x95, 0x08,         //   Report Count (8)
            (byte) 0x81, 0x02,         //   Input(Absolute, Variable, Data) 1 bit x8
            0x75, 0x08,         //   Report Size (8)
            (byte) 0x95, 0x01,         //   Report Count (1)
            (byte) 0x81, 0x01,         //   Input(Absolute, Array, Constant) 1 Byte x1
            0x19, 0x00,         //   Usage Minimum(0)
            0x29, (byte) 0xDD,         //   Usage Maxmum(221)
            0x26, (byte) 0xFF, 0x00,   //   Logical Maximum (255)
            (byte) 0x95, 0x06,         //   Report Count(6)
            (byte) 0x81, 0x00,         //   Input(Absolute, Array, Data) 1 Byte x6
            (byte) 0xC0,               // End Collection
    };
}
