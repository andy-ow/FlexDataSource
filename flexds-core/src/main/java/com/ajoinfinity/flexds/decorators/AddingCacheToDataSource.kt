package com.ajoinfinity.poleconyksiegowy.data.datasource.decorators

import com.ajoinfinity.flexds.Assisted
import com.ajoinfinity.flexds.AssistedFactory
import com.ajoinfinity.flexds.AssistedInject
import com.ajoinfinity.flexds.Cache
import com.ajoinfinity.flexds.DefaultLogger
import com.ajoinfinity.flexds.Logger
import com.ajoinfinity.flexds.DataSource
import com.ajoinfinity.flexds.DataSourceWithCache
import com.ajoinfinity.flexds.FlexDataSource


class AddingCacheToDataSource<D>  constructor(
    private val dsCache: Cache<D>,
    private val dsWithoutCache: DataSource<D>,
    override val logger: Logger = FlexDataSource.defaultLogger,
) : DataSourceWithCache<D> {

    override val dataSourceId: String = "${dsWithoutCache.dataSourceId}<Cache: ${dsCache.dataSourceId}>"
    override val dataTypeName: String = dsWithoutCache.dataTypeName
    override suspend fun containsId(id: String): Result<Boolean> {
        return runCatching {
            val cacheResult = dsCache.containsId(id).getOrThrow()
            val withoutCacheResult = dsWithoutCache.containsId(id).getOrThrow()
            cacheResult || withoutCacheResult
        }
    }

    val mainDsName: String = dsWithoutCache.dsName
    val cacheName: String = dsCache.dsName
    override val dsName = "$mainDsName(Cache: $cacheName)"
    override val SHOULD_NOT_BE_USED_AS_CACHE = dsWithoutCache.SHOULD_NOT_BE_USED_AS_CACHE
    override fun showDataflow() = "${dsCache.showDataflow()}${dsWithoutCache.showDataflow()}"

    private var maxCacheSizeInBytes: Long? = null

    // Cache hit/miss statistics
    private var cacheHits: Int = 0
    private var cacheMisses: Int = 0
    private var totalRetrievals: Int = 0

    init {
        require(dsWithoutCache.dataTypeName == dsCache.dataTypeName) {
            "Datasource and cache must have the same datatype"
        }

//        // Handling errors if dsWithoutCache is already a cached DS
//        require(dsWithoutCache !is DataSourceWithCache) {
//            "$dsName: Internal error: dataSourceWithoutCache already has a cache."
//        }

        require(!dsCache.SHOULD_NOT_BE_USED_AS_CACHE) {
            "$dsName: Internal error: $dsCache is or contains a data source which should never act as a cache."
        }
    }

    // Set the maximum cache size in MB
    override fun cacheSetMaxSize(maxSizeInMB: Long?) {
        maxCacheSizeInBytes = maxSizeInMB?.let { it * 1024 * 1024 }  // Convert MB to bytes
        logger.log("$dsName: Cache max size set to ${maxSizeInMB ?: "unlimited"} MB.")
    }

    // Delete least recently used files based on percentage
    override suspend fun cacheDeleteLeastUsedItemsByPercentage(percentage: Double): Result<Unit> {
        logger.log("$dsName: Attempting to delete least used files by $percentage% of cache.")
        return dsCache.cacheDeleteLeastUsedItemsByPercentage(percentage)
    }

    // Show the current percentage of cache used compared to the maximum size
    override suspend fun cacheShowPercentageUsed(): Result<Double> {
        val currentSizeResult = dsCache.cacheShowPercentageUsed()
        if (maxCacheSizeInBytes == null) {
            logger.log("$dsName: Cache has no size limit. Usage percentage irrelevant.")
            return Result.success(0.0)
        }
        return currentSizeResult
    }

    override suspend fun calculateCurrentCacheSize(): Result<Long> {
        return dsCache.calculateCurrentCacheSize()
    }

    override suspend fun save(id: String, data: D): Result<Unit> {
        val cacheUsagePercentage = cacheShowPercentageUsed().getOrNull() ?: 0.0
        if (maxCacheSizeInBytes != null && cacheUsagePercentage > 100) {
            val error = "$dsName: Cache is full. File cannot be saved locally."
            logger.logError(error)
            return Result.failure(Exception(error))
        }

        val cacheResult = dsCache.save(id, data)
        if (cacheResult.isFailure) {
            logger.logError("$dsName: Failed to save to cache for $dataTypeName $id")
        }
        val remoteResult = dsWithoutCache.save(id, data)

        if (!remoteResult.isSuccess) {
            val error = "$dsName: Failed to save file remotely"
            logger.logError("Internal error: $error")
        }

        return remoteResult
    }

    override suspend fun findById(id: String): Result<D> {
        // Increment total retrievals
        totalRetrievals++

        // Check if it's time to print stats
        if (totalRetrievals % 100 == 0) {
            printCacheStats()
        }

        if (id.isBlank()) {
            return Result.failure(IllegalArgumentException("$dsName: $dataTypeName name cannot be blank"))
        }

        val localResult = dsCache.findById(id)

        if (localResult.isSuccess) {
            cacheHits++
            //logger.log("$name: Cache hit for file: $fileName")
        } else {
            cacheMisses++
            //logger.log("$name: Cache miss for file: $fileName. Retrieving from remote.")
            val remoteResult = dsWithoutCache.findById(id)

            // If the remote retrieval is successful, cache the file locally
            if (remoteResult.isSuccess) {
                dsCache.save(id, remoteResult.getOrNull()!!)
            } else {
                logger.logError("$dsName: $dataTypeName '$id' not available locally or remotely.")
            }
            return remoteResult
        }

        return localResult
    }

    override suspend fun delete(id: String): Result<Unit> {
        val localResult = dsCache.delete(id)
        val remoteResult = dsWithoutCache.delete(id)
        if (localResult.isFailure) logger.logError("$dsName: Failed to delete $dataTypeName $id in cache")
        return if (remoteResult.isSuccess) {
            Result.success(Unit)
        } else {
            val message = "$dsName: Failed to delete $dataTypeName $id remotely"
            logger.logError(message)
            Result.failure(Exception(message))
        }
    }

    override suspend fun listStoredIds(): Result<List<String>> {
        logger.logError("Maybe Cache should store this item list, since we always know if something is saved? Could be improved? We need to check if dscache has this information? What if cache was cleared?")
        return dsWithoutCache.listStoredIds()
        //return dsCache.listStoredIds()
    }

    override suspend fun getTimeLastModification(): Result<Long> {
        // this we can get from cache, even if cache was cleared?
        return dsCache.getTimeLastModification()
    }

    override suspend fun getSize(): Result<Int> {
        // number of items
        logger.logError("Should be improved? Would be much better to have this info in cache.")
        return dsWithoutCache.getSize()
    }

    override suspend fun update(id: String, data: D): Result<Unit> {
        return save(id, data)
    }

    // Print cache hit/miss stats every 100 file retrievals
    private fun printCacheStats() {
        logger.log("$dsName: Cache Stats: $cacheHits hits, $cacheMisses misses after $totalRetrievals retrievals.")
        // Reset the counters after logging
        cacheHits = 0
        cacheMisses = 0
    }
}
