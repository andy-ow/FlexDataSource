package com.ajoinfinity.flexds.decorators

import com.ajoinfinity.flexds.FlexDataSourceManager
import com.ajoinfinity.flexds.Cache
import com.ajoinfinity.flexds.Logger
import com.ajoinfinity.flexds.DataSource
import com.ajoinfinity.flexds.tools.SizeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.ceil


class DsToCacheDecorator<D>(
    val dataSource: DataSource<D>,
    cacheSizeInMB: Long,
    private val getSize: ((D) -> Long)? = null,
    override val logger: Logger = FlexDataSourceManager.logger,
    private val howManyItemsDeleteWhenCacheFullInPercent: Double = 50.0,
    forceNoCheck: Boolean = false,
) : Cache<D> {

    override val SHOULD_NOT_BE_USED_AS_CACHE: Boolean = dataSource.SHOULD_NOT_BE_USED_AS_CACHE

    // Use the provided size function or the default one from SizeUtils
    private fun getSizeOrDefault(data: D): Long {
        return getSize?.invoke(data) ?: SizeUtils.getSize(data)
    }

    override val dsName: String = "${dataSource.dsName}-cache"
    override val dataTypeName: String = dataSource.dataTypeName


    override val dataSourceId: String = dataSource.dataSourceId


    private var maxCacheSizeInBytes: Long? = cacheSizeInMB * 1024 * 1024
    private var currentCacheUsageInBytes: Long = 0L
    init {
        if (SHOULD_NOT_BE_USED_AS_CACHE && !forceNoCheck)
        if (maxCacheSizeInBytes != 0L) {
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                currentCacheUsageInBytes = calculateCurrentCacheSize().getOrThrow()
            }
        }
    }
    private val mutex = Mutex()
    // Set the maximum cache size in MB (convert to bytes)
    override fun cacheSetMaxSize(maxSizeInMB: Long?) {
        maxCacheSizeInBytes = maxSizeInMB?.let { it * 1024 * 1024 }
        logger.log("$dsName: Cache max size set to ${maxSizeInMB ?: "unlimited"} MB.")
    }

    // Delete randomly half of the files
    override suspend fun cacheDeleteLeastUsedItemsByPercentage(percentage: Double): Result<Unit> {
        logger.log("$dsName: Attempting to randomly delete $percentage% of files in the cache.")
        logger.log("$dsName: Current cache usage: $currentCacheUsageInBytes bytes.")
        val listFilesResult = dataSource.listStoredIds()

        if (listFilesResult.isSuccess) {
            val allFiles = listFilesResult.getOrNull() ?: emptyList()
            logger.log("$dsName: Number of items stored: ${allFiles.size}")

            if (allFiles.isNotEmpty()) {
                // Determine the number of files to remove
                val numberOfFilesToRemove = ceil(allFiles.size * (percentage / 100)).toInt()

                // Shuffle the list to randomize the order
                val filesToDelete = allFiles.shuffled().take(numberOfFilesToRemove)

                // Delete the selected files
                filesToDelete.forEach { id ->
                    val deleteResult = dataSource.delete(id)
                    if (deleteResult.isSuccess) {
                        logger.log("$dsName: Deleted $dataTypeName: $id")
                    } else {
                        logger.logError("$dsName: Failed to delete $dataTypeName: $id")
                    }
                }
                logger.log("$dsName: Cleaning finished.")
                logger.log("$dsName: Current cache usage: $currentCacheUsageInBytes bytes.")
                val allFilesAfterCleaning = listFilesResult.getOrNull() ?: emptyList()
                logger.log("$dsName: Number of items stored: ${allFilesAfterCleaning.size}")
                return Result.success(Unit)
            } else {
                logger.log("$dsName: No ${dataTypeName}s found to delete.")
                return Result.success(Unit)
            }
        } else {
            val error = "$dsName: Failed to list ${dataTypeName}s in the cache."
            logger.logError(error)
            return Result.failure(Exception(error))
        }
    }

    // Show the current percentage of cache used compared to max size
    override suspend fun cacheShowPercentageUsed(): Result<Double> {
        if (maxCacheSizeInBytes == null) {
            //logger.log("$cacheName: Cache has no size limit. Usage percentage irrelevant.")
            return Result.success(0.0)
        }

        val percentageUsed = (currentCacheUsageInBytes.toDouble() / maxCacheSizeInBytes!!) * 100
        return Result.success(percentageUsed)
    }

    override suspend fun save(id: String, data: D): Result<Unit> {
        // Cache size logic: check if we can fit the file in cache
        if (maxCacheSizeInBytes != null) {
            if (currentCacheUsageInBytes + getSizeOrDefault(data) > maxCacheSizeInBytes!!) {
                    logger.log("$dsName: File too big to cache.")
                    if ((cacheShowPercentageUsed().getOrNull()?.toInt() ?: 0) > 10) {
                        logger.log("$dsName: Removing some files.")
                        cacheDeleteLeastUsedItemsByPercentage(howManyItemsDeleteWhenCacheFullInPercent)
                }
            }
        }
        // Save the file to the underlying storage
        val result = dataSource.save(id, data)
        if (result.isSuccess) {
            // Update cache size tracking
            currentCacheUsageInBytes += getSizeOrDefault(data)
        } else {
            logger.log("$dsName: Cannot save file in cache.")
        }
        return result
    }

    override suspend fun delete(id: String): Result<Unit> {
        return mutex.withLock {
            val itemResult = dataSource.findById(id)
            if (itemResult.isSuccess) {
                val itemSize = getSizeOrDefault(itemResult.getOrThrow())
                val deleteResult = dataSource.delete(id)
                if (deleteResult.isSuccess) {
                    currentCacheUsageInBytes -= itemSize
                    Result.success(Unit)
                } else {
                    deleteResult
                }
            } else {
                Result.failure(itemResult.exceptionOrNull() ?: Exception("Internal error: Exception was null"))
            }
        }
    }

    override suspend fun deleteAll(): Result<Unit> {
        return dataSource.deleteAll()
    }

    override suspend fun calculateCurrentCacheSize(): Result<Long> {
        mutex.withLock {
            if (maxCacheSizeInBytes == 0L) return Result.success(0L)

            val idsResult = dataSource.listStoredIds()
            if (idsResult.isSuccess) {
                val ids = idsResult.getOrThrow()
                var totalSize = 0L

                for (id in ids) {
                    val itemResult = dataSource.findById(id)
                    if (itemResult.isFailure) {
                        val message = "$dsName: Failed to find item with id $id while calculating cache size."
                        logger.logError(message)
                        return Result.failure(itemResult.exceptionOrNull() ?: IllegalStateException(message))
                    }
                    totalSize += getSizeOrDefault(itemResult.getOrThrow())
                }

                currentCacheUsageInBytes = totalSize
                return Result.success(currentCacheUsageInBytes)
            } else {
                val message = "$dsName: Failed to list stored IDs while calculating cache size."
                logger.logError(message)
                return Result.failure(IllegalStateException(message))
            }
        }
    }

    override suspend fun findById(id: String): Result<D> {
        return dataSource.findById(id)
    }

    override suspend fun update(id: String, data: D): Result<Unit> {
        return dataSource.update(id, data)
    }

    override suspend fun containsId(id: String): Result<Boolean> {
        return dataSource.containsId(id)
    }

    override suspend fun listStoredIds(): Result<List<String>> {
        return dataSource.listStoredIds()
    }

    override suspend fun getTimeLastModification(): Result<Long> {
        return dataSource.getTimeLastModification()
    }

    override suspend fun getNumberOfElements(): Result<Int> {
        return dataSource.getNumberOfElements()
    }
}
