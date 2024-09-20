package com.ajoinfinity.flexds.features.maxsize

import com.ajoinfinity.flexds.Flexds
import com.ajoinfinity.flexds.features.FlexdsMaxSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MaxSizeDecorator<D>(
    override val fds: Flexds<D>,
    private val initialMaxSize: Long,
    private val percentToRemove: Double = 0.5, // Default: remove half when exceeding max size
    private val shouldPreventSaveWhenExceeded: Boolean = false, // If true, prevent save or update when exceeding max size
    private val maxSizeDelegate: MaxSizeDelegate<D> = MaxSizeDelegate(fds, initialMaxSize,)
) : BaseMaxSizeDecorator<D>(fds), FlexdsMaxSize by maxSizeDelegate {


    init {
        CoroutineScope(Dispatchers.IO).launch {
            maxSizeDelegate.initializeCurrentSize()
        }
    }


    override suspend fun save(id: String, data: D): Result<D> {
        val dataSize = fds.getItemSize(id)
        return if (willExceedMaxSize(dataSize).getOrNull() ?: true) {
            handleExceedingMaxSize(id, data, dataSize)
        } else {
            performSave(id, data, dataSize)
        }
    }

    override suspend fun update(id: String, data: D): Result<D> {
        return save(id, data)
    }

    override suspend fun delete(id: String): Result<String> {
        val itemSize = fds.getItemSize(id)
        val result = fds.delete(id)
        if (result.isSuccess) {
            try {
                maxSizeDelegate.currentSize -= itemSize.getOrThrow()
            } catch(e: Exception) {
                logger.logError("MaxSizeDecorator: delete: Could not get size of $id")
                maxSizeDelegate.initializeCurrentSize()
            }
        }
        return result
    }

    // Helper methods
    private suspend fun performSave(id: String, data: D, dataSize: Result<Long>): Result<D> {
        val result = fds.save(id, data)
        if (result.isSuccess && dataSize.isSuccess) {
            maxSizeDelegate.currentSize += dataSize.getOrThrow()
        } else {
            if (result.isFailure) logger.logError("Could not save $dataTypeName $id")
            if (dataSize.isFailure) logger.logError("Could not determine size of $dataTypeName $id")
        }
        return result
    }

    private suspend fun handleExceedingMaxSize(id: String, data: D, dataSize: Result<Long>): Result<D> {
        return if (shouldPreventSaveWhenExceeded) {
            Result.failure(Exception("Max size exceeded. Cannot save data for $dataTypeName $id"))
        } else {
            dataSize.getOrNull()?.let { freeUpSpace(it) }
            performSave(id, data, dataSize)
        }
    }

    private fun willExceedMaxSize(dataSize: Result<Long>): Result<Boolean> {
        return try {
            Result.success(maxSizeDelegate.currentSize + dataSize.getOrThrow() > maxSizeDelegate.maxSize)
        } catch(e: Exception) {
            Result.failure(IllegalArgumentException("Could not determine $dataTypeName size"))
        }
    }

    private suspend fun freeUpSpace(requiredSize: Long) {
        val spaceToFree = (requiredSize + maxSizeDelegate.currentSize - maxSizeDelegate.maxSize)
        val targetFreeSpace = (spaceToFree * percentToRemove).toLong()

        val storedIds = fds.listStoredIds().getOrNull() ?: return
        var freedSpace = 0L

        for (id in storedIds) {
            val itemSize = fds.getItemSize(id).getOrElse { 0 }
            fds.delete(id)
            freedSpace += itemSize
            maxSizeDelegate.currentSize -= itemSize

            if (freedSpace >= targetFreeSpace) {
                break
            }
        }

        logger.log("MaxSizeDecorator: Freed $freedSpace bytes to make space for new data.")
    }
}
