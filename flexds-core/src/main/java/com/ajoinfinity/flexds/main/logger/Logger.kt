package com.ajoinfinity.flexds.main.logger

interface Logger {
    fun log(message: String)
    fun logError(message: String, throwable: Throwable?)
}