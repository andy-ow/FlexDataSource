package com.ajoinfinity.flexds.features

interface FlexdsMaxSize {
    suspend fun getFlexdsUsageInPercentage(): Result<Double>
    suspend fun setMaxSize(newMaxSize: Long): Result<Unit>
    suspend fun getMaxSize(): Result<Long>

}