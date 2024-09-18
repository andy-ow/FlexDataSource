// Mapper.kt
package com.ajoinfinity.flexds

interface Mapper<D, E> {
    fun fromDataSource(d: D): E
    fun toDataSource(e: E): D
    fun getName() = "Mapper"
}
