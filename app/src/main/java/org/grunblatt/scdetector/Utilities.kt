package org.grunblatt.scdetector

import java.nio.ByteBuffer

fun String.splitToByteArray(): ByteArray {
    val split = split('.')
    val byteBuffer = ByteBuffer.allocate(split.size)
    split.forEach {
        byteBuffer.put(it.toByte())
    }
    return byteBuffer.array()
}

fun printB(bytes: ByteArray): String? {
    val sb = StringBuilder()
    sb.append("0x")
    for (b in bytes) {
        sb.append(String.format("%02X", b))
    }
    return sb.toString()
}