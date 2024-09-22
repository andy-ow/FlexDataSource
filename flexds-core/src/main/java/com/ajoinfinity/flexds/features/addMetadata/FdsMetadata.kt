package com.ajoinfinity.flexds.features.addMetadata

@JvmInline
value class FdsMetadata(val value: String) {

    fun toLong(): Long {
        return value.toLong()  // Converts the underlying String to a Long
    }

    fun toInt(): Int {
        return value.toInt()  // Converts the underlying String to an Int
    }

    fun toDouble(): Double {
        return value.toDouble()  // Converts the underlying String to a Double
    }

    override fun toString(): String {
        return value  // Returns the underlying String value
    }
}