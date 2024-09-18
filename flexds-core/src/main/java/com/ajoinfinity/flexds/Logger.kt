package com.ajoinfinity.flexds

interface Logger {
    fun log(message: String)
    fun logError(message: String, throwable: Throwable? = null)
}