package com.ajoinfinity.poleconyksiegowy.data.datasource.decorators

import java.io.File

object SizeUtils {

    fun getSizeOfByteArray(data: ByteArray): Long = data.size.toLong()

    fun getSizeOfString(data: String): Long = data.toByteArray(Charsets.UTF_8).size.toLong()

    fun getSizeOfFile(file: File): Long = file.length()



    fun getSizeOfInt(data: Int): Long = Integer.BYTES.toLong()

    fun getSizeOfLong(data: Long): Long = java.lang.Long.BYTES.toLong()

    fun getSizeOfFloat(data: Float): Long = java.lang.Float.BYTES.toLong()

    fun getSizeOfDouble(data: Double): Long = java.lang.Double.BYTES.toLong()

    // Fallback for any type that doesn't have an easy size calculation.
    fun <D> defaultSize(data: D): Long { // Default fallback
        throw IllegalArgumentException("getSize method must be provided unless cache has no limits (cacheSizeInMB = 0)")
    }
    inline fun <D> getSize(data: D): Long {
        return when (data) {
            is ByteArray -> getSizeOfByteArray(data)
            is String -> getSizeOfString(data)
            is File -> getSizeOfFile(data)
            is Int -> getSizeOfInt(data)
            is Long -> getSizeOfLong(data)
            is Float -> getSizeOfFloat(data)
            is Double -> getSizeOfDouble(data)
            else -> defaultSize(data)
        }
    }
}