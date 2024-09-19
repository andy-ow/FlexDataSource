package com.ajoinfinity.flexds.features.maxsize

import com.ajoinfinity.flexds.Flexds
import com.ajoinfinity.flexds.features.FlexdsMaxSize

class MaxSizeDelegate<D>(
    internal val fds: Flexds<D>,
    internal var maxSize: Long,
): FlexdsMaxSize {

    internal var currentSize: Long = 0

    internal suspend fun initializeCurrentSize() {
        currentSize = fds.getFlexdsSize().getOrElse { 0 }
    }

    override suspend fun getMaxSize(): Result<Long> {
        return Result.success(maxSize)
    }

    override suspend fun setMaxSize(newMaxSize: Long): Result<Unit> {
        maxSize = newMaxSize
        return Result.success(Unit)
    }

    override suspend fun getFlexdsUsageInPercentage(): Result<Double> {
        return if (maxSize > 0) {
            Result.success((fds.getFlexdsSize().getOrThrow().toDouble() / maxSize) * 100)
        } else {
            Result.failure(IllegalStateException("Could not get current size of ${fds.name}"))
        }
    }

}