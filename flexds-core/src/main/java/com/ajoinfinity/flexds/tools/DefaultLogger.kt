package com.ajoinfinity.flexds.tools

import com.ajoinfinity.flexds.Logger

class DefaultLogger: Logger {
    override fun log(message: String) {
        println(message)
    }

    override fun logError(message: String, throwable: Throwable?) {
        println("Error: $message, throwable.message: ${throwable?.message}, throwable.cause: ${throwable?.cause}")
    }
}