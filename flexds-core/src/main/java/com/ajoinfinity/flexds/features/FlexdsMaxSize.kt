package com.ajoinfinity.flexds.features

interface FlexdsMaxSize {
    suspend fun getFlexdsUsageInPercentage(): Result<Double>{
        throw NotImplementedError("Feature is not implemented. Please use MaxSizeDecorator")
    }
    suspend fun setMaxSize(newMaxSize: Long): Result<Unit>{
        throw NotImplementedError("Feature is not implemented. Please use MaxSizeDecorator")
    }
    suspend fun getMaxSize(): Result<Long>{
        throw NotImplementedError("Feature is not implemented. Please use MaxSizeDecorator")
    }

}