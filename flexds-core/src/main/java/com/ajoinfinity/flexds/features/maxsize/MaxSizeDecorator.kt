package com.ajoinfinity.flexds.features.maxsize

import com.ajoinfinity.flexds.main.Flexds

class MaxSizeDecorator<D>(
    val fds: Flexds<D>,
    private val initialMaxSize: Long,
    private val percentToRemove: Double = 0.5, // Default: remove half when exceeding max size
    private val shouldPreventSaveWhenExceeded: Boolean = false, // If true, prevent save or update when exceeding max size
) : Flexds<D> by fds {

    private var maxSize: Long = initialMaxSize
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
            Result.failure(IllegalStateException("Could not get current size of ${fds.fdsId}"))
        }
    }

    override suspend fun save(id: String, data: D): Result<D> {
        val dataSize = getItemSize(data)
        return if (willExceedMaxSize(dataSize).getOrNull() != false) {
            handleExceedingMaxSize(id, data, dataSize)
        } else {
            fds.save(id, data)
        }
    }

    override suspend fun update(id: String, data: D): Result<D> {
        // Get the current size of the item
        val oldSize = fds.getItemSize(id)

        // Get the new size of the updated data
        val newSize = fds.getItemSize(data)

        return if (oldSize.isSuccess && newSize.isSuccess) {
            val sizeDifference = newSize.getOrThrow() - oldSize.getOrThrow()
            if (willExceedMaxSize(Result.success(sizeDifference)).getOrNull() != false) {
                handleExceedingMaxSize(id, data, Result.success(sizeDifference))
            } else {
                fds.update(id, data)
            }
        } else {
            logger.logError("Could not determine old or new size for ${dataClazz.simpleName} $id", null)
            Result.failure(Exception("Size calculation error"))
        }
    }

    private suspend fun handleExceedingMaxSize(
        id: String,
        data: D,
        dataSize: Result<Long>
    ): Result<D> {
        return if (shouldPreventSaveWhenExceeded) {
            Result.failure(Exception("Max size exceeded. Cannot save data for ${dataClazz.simpleName} $id"))
        } else {
            dataSize.getOrNull()?.let { freeUpSpace(it) }
            fds.save(id, data)
        }
    }

    private suspend fun willExceedMaxSize(dataSize: Result<Long>): Result<Boolean> {
        return try {
            Result.success(
                fds.getFlexdsSize().getOrThrow() + dataSize.getOrThrow() > maxSize
            )
        } catch (e: Exception) {
            Result.failure(IllegalArgumentException("Could not determine ${dataClazz.simpleName} size"))
        }
    }

    private suspend fun freeUpSpace(requiredSize: Long) {
//            fds.deleteAll()
//            return
            val spaceToFree =
                (requiredSize + fds.getFlexdsSize().getOrThrow() - maxSize)
            val targetFreeSpace = spaceToFree
            val storedIds = fds.listStoredIds().getOrNull() ?: return
            var freedSpace = 0L
            for (id in storedIds) {
                val itemSize = fds.getItemSize(id).getOrElse { 0 }
                fds.delete(id)
                freedSpace += itemSize

                if (freedSpace >= targetFreeSpace) {
                    break
                }
            }

        //logger.log("MaxSizeDecorator: Freed $freedSpace bytes to make space for new data.")
    }
}
