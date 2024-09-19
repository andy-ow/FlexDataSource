package com.ajoinfinity.flexds.features

interface FlexdsSize {
    suspend fun getFlexdsSize(): Result<Long> {
        throw NotImplementedError("Feature is not implemented. Please use SizeDecorator.")
    }
    suspend fun getItemSize(id: String): Result<Long>
    {
        throw NotImplementedError("Feature is not implemented. Please use SizeDecorator")
    }
}