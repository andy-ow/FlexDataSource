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
        val dataSize = fds.getItemSize(id).getOrElse { getSizeOfData(data) }

        return if (willExceedMaxSize(dataSize)) {
            handleExceedingMaxSize(id, data, dataSize)
        } else {
            performSave(id, data, dataSize)
        }
    }

    override suspend fun update(id: String, data: D): Result<D> {
        return save(id, data)
    }

    override suspend fun delete(id: String): Result<String> {
        val itemSize = fds.getItemSize(id).getOrElse { 0 }
        val result = fds.delete(id)
        if (result.isSuccess) {
            maxSizeDelegate.currentSize -= itemSize
        }
        return result
    }

    // Helper methods
    private suspend fun performSave(id: String, data: D, dataSize: Long): Result<D> {
        val result = fds.save(id, data)
        if (result.isSuccess) {
            maxSizeDelegate.currentSize += dataSize
        }
        return result
    }

    private suspend fun handleExceedingMaxSize(id: String, data: D, dataSize: Long): Result<D> {
        return if (shouldPreventSaveWhenExceeded) {
            Result.failure(Exception("Max size exceeded. Cannot save data: $id"))
        } else {
            freeUpSpace(dataSize)
            performSave(id, data, dataSize)
        }
    }

    private fun willExceedMaxSize(dataSize: Long): Boolean {
        return (maxSizeDelegate.currentSize + dataSize) > maxSizeDelegate.maxSize
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

    private fun getSizeOfData(data: D): Long {
        // Placeholder logic to determine size of the data.
        // Replace this with actual logic for determining the size.
        return 1L // Assuming each data element has a size of 1 for simplicity.
    }

}
