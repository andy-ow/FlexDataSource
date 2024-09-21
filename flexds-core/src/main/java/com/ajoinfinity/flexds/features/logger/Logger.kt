package com.ajoinfinity.flexds.features.logger

interface Logger {
    fun log(message: String)
    fun logError(message: String, throwable: Throwable? = null)
}