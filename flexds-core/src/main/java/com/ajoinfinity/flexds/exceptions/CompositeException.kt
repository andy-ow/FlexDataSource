package com.ajoinfinity.flexds.exceptions

class CompositeException(val errors: List<Throwable>) : Exception() {
    override val message: String
        get() = errors.joinToString(", ") { it.message ?: "Unknown error" }
}