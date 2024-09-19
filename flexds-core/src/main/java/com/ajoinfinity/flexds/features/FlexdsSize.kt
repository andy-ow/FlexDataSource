package com.ajoinfinity.flexds.features

interface FlexdsSize {
    suspend fun getFlexDsSize(): Result<Long> {
        throw NotImplementedError("Feature is not implemented. Please use DsWithSizeDecorator")
    }
}