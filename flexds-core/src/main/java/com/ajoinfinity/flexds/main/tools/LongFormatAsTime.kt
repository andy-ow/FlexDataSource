package com.ajoinfinity.flexds.main.tools

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


fun String.formatAsTime(): String {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    val date = Date(this)
    return dateFormat.format(date)
}

fun Long.formatAsTime(): String {

    return try {
        this.toString().formatAsTime()
    } catch(e: Exception) {
        println("Error: '$this' as string: '${this.toString()}' cannot formatAsTime, because: $e")
        return this.toString()
    }
}
