package com.github.asforest.blew.util

object BinaryUtils {
    val Short.low8: Byte get() = (this.toInt() and 0xff).toByte()
    val Short.high8: Byte get() = (this.toInt() shr 8).toByte()
    val Short.reversedHighLowByte: Short get() = ((low8.toInt() shl 8) + high8).toShort()
}