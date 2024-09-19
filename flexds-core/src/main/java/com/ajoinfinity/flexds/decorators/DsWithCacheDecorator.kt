package com.ajoinfinity.flexds.decorators

import com.ajoinfinity.flexds.Cache
import com.ajoinfinity.flexds.Logger
import com.ajoinfinity.flexds.DataSource
import com.ajoinfinity.flexds.datasources.DataSourceWithCache
import com.ajoinfinity.flexds.FlexDataSourceManager


class DsWithCacheDecorator<D>  constructor(
    private val dsCache: Cache<D>,
    private val dsWithoutCache: DataSource<D>,
    override val logger: Logger = FlexDataSourceManager.logger,
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
        dsCache.cacheSetMaxSize(maxSizeInMB)
    }

    // Delete least recently used files based on percentage
    override suspend fun cacheDeleteLeastUsedItemsByPercentage(percentage: Double): Result<Unit> {
        logger.log("$dsName: Attempting to delete least used files by $percentage% of cache.")
        return dsCache.cacheDeleteLeastUsedItemsByPercentage(percentage)
    }

    // Show the current percentage of cache used compared to the maximum size
    override suspend fun cacheShowPercentageUsed(): Result<Double> {
        return dsCache.cacheShowPercentageUsed()
    }

    override suspend fun calculateCurrentCacheSize(): Result<Long> {
        return dsCache.calculateCurrentCacheSize()
    }

    override suspend fun save(id: String, data: D): Result<Unit> {
        val cacheResult = dsCache.save(id, data)
        if (cacheResult.isFailure) {
            logger.logError("$dsName: Failed to save to cache for $dataTypeName $id")
        }
        val remoteResult = dsWithoutCache.save(id, data)

        if (!remoteResult.isSuccess) {
            val error = "$dsName: Failed to save $dataTypeName"
            logger.logError("Internal error: $error")
        }
        return remoteResult
    }

    override suspend fun findById(id: String): Result<D> {
        // Check if it's time to print stats
        if (totalRetrievals % 5000 == 0) {
            printCacheStats()
        }
        // Increment total retrievals
        totalRetrievals++

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
                logger.logError("$dsName: $dataTypeName '$id' not available here or in cache ${dsCache.dsName}.")
            }
            return remoteResult
        }

        return localResult
    }

    override suspend fun delete(id: String): Result<Unit> {
        val localResult = dsCache.delete(id)
        val remoteResult = dsWithoutCache.delete(id)
        if (localResult.isFailure) logger.logError("$dsName: Failed to delete $dataTypeName $id in cache ${dsCache.dsName}.")
        return if (remoteResult.isSuccess) {
            Result.success(Unit)
        } else {
            val message = "$dsName: Failed to delete $dataTypeName $id "
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

    override suspend fun getNumberOfElements(): Result<Int> {
        // number of items
        logger.logError("Should be improved? Would be much better to have this info in cache.")
        return dsWithoutCache.getNumberOfElements()
    }

    override suspend fun update(id: String, data: D): Result<Unit> {
        return save(id, data)
    }

    override suspend fun clearCache(): Result<Unit> {
        return dsCache.deleteAll()
    }

    override suspend fun deleteAll(): Result<Unit> {
        val resultClearCache = clearCache()
        val resultDsDeleteAll = dsWithoutCache.deleteAll()

        return if (resultClearCache.isSuccess && resultDsDeleteAll.isSuccess) {
            Result.success(Unit)
        } else {
            val failureReason = resultClearCache.exceptionOrNull() ?: resultDsDeleteAll.exceptionOrNull()
            Result.failure(failureReason ?: Exception("Unknown failure"))
        }
    }

    // Print cache hit/miss stats every 100 file retrievals
    private fun printCacheStats() {
        val successRate: Int = (100 * cacheHits.toFloat() / totalRetrievals.toFloat()).toInt()
        logger.log("$dsName: Cache Stats: ${successRate}% success rate, $cacheHits hits, $cacheMisses misses after $totalRetrievals retrievals.")
        // Reset the counters after logging
        //cacheHits = 0
        //cacheMisses = 0
    }
}
