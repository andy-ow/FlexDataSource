package com.ajoinfinity.flexds.features.fdssize

interface FlexDataSourceSize {
    suspend fun getFlexDsSize(): Result<Long> {
        throw NotImplementedError("Feature is not implemented. Please use DsWithSizeDecorator")
    }
}