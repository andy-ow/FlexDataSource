package com.ajoinfinity.flexds

interface Cache<D> : FlexDataSource<D> {
    fun cacheSetMaxSize(maxSizeInMB: Long?)
    suspend fun cacheDeleteLeastUsedItemsByPercentage(percentage: Double): Result<Unit>
    suspend fun cacheShowPercentageUsed(): Result<Double>
    suspend fun calculateCurrentCacheSize(): Result<Long>
}