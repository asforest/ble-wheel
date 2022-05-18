package com.example.w2.hid;

public class HidBase
{
    /**
            * Main items
     */
    public static byte INPUT(final int size) { return (byte) (0x80 | size); }
    public static byte OUTPUT(final int size) { return (byte) (0x90 | size); }
    public static byte COLLECTION(final int size) { return (byte) (0xA0 | size); }
    public static byte FEATURE(final int size) { return (byte) (0xB0 | size); }
    public static byte END_COLLECTION(final int size) { return (byte) (0xC0 | size); }

    /**
     * Global items
     */
    public static byte USAGE_PAGE(final int size) { return (byte) (0x04 | size); }
    public static byte LOGICAL_MINIMUM(final int size) { return (byte) (0x14 | size); }
    public static byte LOGICAL_MAXIMUM(final int size) { return (byte) (0x24 | size); }
    public static byte PHYSICAL_MINIMUM(final int size) { return (byte) (0x34 | size); }
    public static byte PHYSICAL_MAXIMUM(final int size) { return (byte) (0x44 | size); }
    public static byte UNIT_EXPONENT(final int size) { return (byte) (0x54 | size); }
    public static byte UNIT(final int size) { return (byte) (0x64 | size); }
    public static byte REPORT_SIZE(final int size) { return (byte) (0x74 | size); }
    public static byte REPORT_ID(final int size) { return (byte) (0x84 | size); }
    public static byte REPORT_COUNT(final int size) { return (byte) (0x94 | size); }

    /**
     * Local items
     */
    public static byte USAGE(final int size) { return (byte) (0x08 | size); }
    public static byte USAGE_MINIMUM(final int size) { return (byte) (0x18 | size); }
    public static byte USAGE_MAXIMUM(final int size) { return (byte) (0x28 | size); }

    public static byte LSB(final int value) {return (byte) (value & 0xff); }
    public static byte MSB(final int value) {return (byte) (value >> 8 & 0xff); }

}
