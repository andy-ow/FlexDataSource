package com.ajoinfinity.flexds.features

interface FlexdsDelete<T> {
    suspend fun delete(id: String): Result<String>

}
