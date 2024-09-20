package com.ajoinfinity.flexds.features.maxsize

import com.ajoinfinity.flexds.Flexds
import com.ajoinfinity.flexds.features.FlexdsMaxSize
import com.ajoinfinity.flexds.features.size.SizeDecorator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MaxSizeDecorator<D>(
    override val fds: SizeDecorator<D>,
    private val initialMaxSize: Long,
    private val percentToRemove: Double = 0.5, // Default: remove half when exceeding max size
    private val shouldPreventSaveWhenExceeded: Boolean = false, // If true, prevent save or update when exceeding max size
    private val maxSizeDelegate: MaxSizeDelegate<D> = MaxSizeDelegate(fds, initialMaxSize)
) : BaseMaxSizeDecorator<D>(fds), FlexdsMaxSize by maxSizeDelegate {

    override suspend fun save(id: String, data: D): Result<D> {

        val dataSize = fds.getItemSize(data)
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
            logger.logError("Could not determine old or new size for $dataTypeName $id")
            Result.failure(Exception("Size calculation error"))
        }
    }



    private suspend fun handleExceedingMaxSize(id: String, data: D, dataSize: Result<Long>): Result<D> {
        return if (shouldPreventSaveWhenExceeded) {
            Result.failure(Exception("Max size exceeded. Cannot save data for $dataTypeName $id"))
        } else {
            dataSize.getOrNull()?.let { freeUpSpace(it) }
            fds.save(id, data)
        }
    }

    private suspend fun willExceedMaxSize(dataSize: Result<Long>): Result<Boolean> {
        return try {
            Result.success(fds.getFlexdsSize().getOrThrow() + dataSize.getOrThrow() > maxSizeDelegate.maxSize)
        } catch (e: Exception) {
            Result.failure(IllegalArgumentException("Could not determine $dataTypeName size"))
        }
    }

    private suspend fun freeUpSpace(requiredSize: Long) {
        println("FreeUpSpace")
        val targetFreeSpace = (fds.getFlexdsSize().getOrThrow() * percentToRemove).toLong()
        println("targetFreeSpace: $targetFreeSpace")
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
        println("Freed space: $freedSpace")
        logger.log("MaxSizeDecorator: Freed $freedSpace bytes to make space for new data.")
    }
}
