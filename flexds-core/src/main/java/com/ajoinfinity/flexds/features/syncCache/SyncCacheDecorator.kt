package com.ajoinfinity.flexds.features.syncCache

import com.ajoinfinity.flexds.features.addMetadata.FdsMetadata
import com.ajoinfinity.flexds.main.Flexds

class SyncCacheDecorator<D>(
    val fds: Flexds<D>,
) : Flexds<D> by fds {
    override val cache: Flexds<D> = fds.cache

    private val lastCacheSyncTimeMetaName = "lastCacheSyncTime"

    override suspend fun syncCache(): Result<Unit> {
        val startSyncTime = FdsMetadata(System.currentTimeMillis().toString())
        return try {
            val itemIds = listStoredIds().getOrThrow().shuffled()
            val cacheNotOk: MutableSet<Flexds<D>> = mutableSetOf()

            val results = mutableListOf<Result<D>>()

            // Iterate through each item and try to save it in caches
            for (itemId in itemIds) {
                try {
                    val item = findById(itemId).getOrThrow()
                    for (cache in setOfCaches) {
                        try {
                            results.add(cache.save(itemId, item))
                        } catch (e: Exception) {
                            cacheNotOk.add(cache)
                            logger.logError("SyncCache: Could not save ${dataClazz.simpleName} $itemId in cache ${cache.fdsId}", e)
                            results.add(Result.failure(e))
                        }
                    }
                } catch (e: Exception) {
                    logger.logError("SyncCache: Could not retrieve ${dataClazz.simpleName} $itemId", e)
                    results.add(Result.failure(e))
                }
            }

            // Update the last sync time metadata for successful caches
            for (cache in setOfCaches) {
                if (cache !in cacheNotOk) {
                    try {
                        cache.saveMetadata(lastCacheSyncTimeMetaName, startSyncTime)
                    } catch (e: Exception) {
                        logger.logError("Could not save lastCacheSyncTime for cache ${cache.fdsId}", e)
                    }
                }
            }

            // Check if all results were successful
            if (results.all { it.isSuccess }) {
                Result.success(Unit)
            } else {
                Result.failure(IllegalStateException("Could not sync cache: errors occurred during syncing"))
            }

        } catch (e: Exception) {
            logger.logError("SyncCache: Could not sync cache", e)
            Result.failure(e)
        }
    }
}
