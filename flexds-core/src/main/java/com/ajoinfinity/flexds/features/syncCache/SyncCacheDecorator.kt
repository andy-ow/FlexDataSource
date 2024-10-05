package com.ajoinfinity.flexds.features.syncCache

import com.ajoinfinity.flexds.features.addMetadata.FdsMetadata
import com.ajoinfinity.flexds.main.Flexds
import com.ajoinfinity.flexds.main.tools.formatAsTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.cancellation.CancellationException

class SyncCacheDecorator<D>(
    val fds: Flexds<D>,
) : Flexds<D> by fds {
    override val cache: Flexds<D> = fds.cache

    override val lastCacheSyncTimeMetaName = "lastCacheSyncTime"
    // Mutex to synchronize access to syncJob
    private val mutex = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    // Deferred representing the ongoing synchronization job
    @Volatile
    private var syncJob: Deferred<Result<Unit>>? = null
    override suspend fun syncCachesIfNeeded(): Result<Unit> {
        logger.log("SyncCache: syncCachesIfNeeded called for $fdsId")
        return privateSyncCachesIfNeeded()
        // Synchronize access to syncJob using Mutex
        mutex.withLock {
            // If a sync job is already in progress, return it
            syncJob?.takeIf { it.isActive }?.let {
                logger.log("SyncCache: Sync of caches for $fdsId already scheduled.")
                return it.await()
            }

            // Start a new sync job with a 10-second delay
            syncJob = scope.async {
                try {
                    logger.log("SyncCache: In 1 seconds starting sync job for $fdsId")
                    delay(1_000) // Wait for 1 seconds
                    logger.log("SyncCache: Starting sync job for $fdsId")
                    privateSyncCachesIfNeeded()
                } catch (e: CancellationException) {
                    // Handle coroutine cancellation if needed
                    logger.logError("SyncCache: Sync job was cancelled", e)
                    Result.failure(e)
                } catch (e: Exception) {
                    // Log any unexpected exceptions
                    logger.logError("SyncCache: Exception during sync", e)
                    Result.failure(e)
                }
            }

            // Setup to clear the syncJob once done
            syncJob!!.invokeOnCompletion { throwable ->
                scope.launch {
                    mutex.withLock {
                        if (syncJob?.isCompleted == true) {
                            syncJob = null
                        }
                    }
                }
            }

            // Return the newly started job's result
            return syncJob!!.await()
        }
    }

    suspend fun privateSyncCachesIfNeeded(): Result<Unit> {
        // Retrieve last sync times for caches
        val cachesTimesResult = getLastCachesSyncTime()
        if (cachesTimesResult.isFailure) {
            return Result.failure(
                cachesTimesResult.exceptionOrNull() ?: Exception("SyncCache: Error while retrieving caches sync times")
            )
        }
        val cachesTimes = cachesTimesResult.getOrThrow() // List<Pair<Flexds<D>, Result<Long>>>

        // Retrieve the last modification time of the database
        val dbLastUpdateResult = getLastModificationTime()
        if (dbLastUpdateResult.isFailure) {
            return Result.failure(
                dbLastUpdateResult.exceptionOrNull() ?: Exception("SyncCache: Error while retrieving database last modification time")
            )
        }
        val dbLastUpdate = dbLastUpdateResult.getOrThrow()
        logger.log("SyncCache: Db $fdsId last modification time: ${dbLastUpdate.formatAsTime()}")
        // Prepare a list to collect caches that need to be updated
        val cachesToUpdate = mutableListOf<Flexds<D>>()
        val errors = mutableListOf<Throwable>()

        // Iterate over each cache and determine if it needs updating
        for ((cache, lastSyncTimeResult) in cachesTimes) {
            if (lastSyncTimeResult.isSuccess) {
                val lastSyncTime = lastSyncTimeResult.getOrThrow()
                if (dbLastUpdate > lastSyncTime) {
                    logger.log("SyncCache: Need to sync ${cache.fdsId}, it was last synced ${lastSyncTime.formatAsTime()}")
                    cachesToUpdate.add(cache)
                } else {
                    logger.log("SyncCache: No need to sync ${cache.fdsId}, it was last synced ${lastSyncTime.formatAsTime()}")
                }
            } else {
                // Collect exceptions from caches whose last sync time retrieval failed
                val exception = lastSyncTimeResult.exceptionOrNull()
                    ?: Exception("SyncCache: Unknown error while retrieving last sync time for cache")
                errors.add(exception)
                // Optionally log the error
                logger.logError("SyncCache: Failed to retrieve last sync time for cache ${cache.fdsId}", exception)
            }
        }

        // If there were errors retrieving last sync times, you can decide how to handle them
        if (errors.isNotEmpty()) {
            // You might choose to return a failure here or continue with available caches
            // For this example, we'll proceed with available caches and log errors
        }

        // Synchronize caches that need updating
        return if (cachesToUpdate.isNotEmpty()) {
            val syncResult = syncCache(cachesToUpdate)
            if (syncResult.isFailure) {
                // Handle sync failures
                Result.failure(
                    syncResult.exceptionOrNull() ?: Exception("SyncCache: Error during cache synchronization")
                )
            } else {
                Result.success(Unit)
            }
        } else {
            // No caches need updating
            Result.success(Unit)
        }
    }


    override suspend fun getLastCachesSyncTime(): Result<Set<Pair<Flexds<D>, Result<Long>>>> {
        return try {
            val syncTimes = mutableSetOf<Pair<Flexds<D>, Result<Long>>>()
            for (cache in setOfCaches) {
                if (cache.containsMetadataId(lastCacheSyncTimeMetaName).getOrNull() == true) {
                    val lastSyncTime = cache.findByIdMetadata(lastCacheSyncTimeMetaName).map { it.toString().toLong() }
                    syncTimes.add(Pair(cache, lastSyncTime))
                    logger.log("SyncCache: Cache ${cache.fdsId} last sync time: ${lastSyncTime.getOrNull()?.formatAsTime()}")
                } else { // cache was never synced
                    logger.log("SyncCache: Cache ${cache.fdsId} was never synced.")
                    syncTimes.add(Pair(cache, Result.success(0)))
                }
            }
            Result.success(syncTimes)
        } catch(e: Exception) {
            logger.logError("SyncCache: Could not get last sync time of caches",e)
            Result.failure(e)
        }
    }

    private suspend fun deleteUnnecessaryItems() {
        logger.log("SyncCache: For db ${fdsId}, removing old items from caches. Start")
        try {
            val dbItems = listStoredIds().getOrThrow()
            for (cache in setOfCaches) {
                logger.log("SyncCache: For db ${fdsId}, removing old items from cache ${cache.fdsId}")
                try {
                    cache.listStoredIds().getOrThrow().filter { !dbItems.contains(it) || unmutable == false }.also {
                        if (it.isNotEmpty()) logger.log("""SyncCache: For db ${fdsId}, removing ${it.joinToString(", ")} from ${cache.fdsId}""")
                        else logger.log("SyncCache: For db ${fdsId}, nothing to remove in ${cache.fdsId}")
                    }
                        .forEach {
                            logger.log("SyncCache: Deleting $it from ${cache.fdsId}")
                            cache.delete(it) }
                    logger.log("SyncCache: Done with deleting old items from ${cache.fdsId} for ${fdsId}")
                } catch (e: Exception) {
                    logger.logError("SyncCache: Error while removing old items from cache ${cache.fdsId}", e)
                }
            }
            logger.log("SyncCache: Done with deleting old items from caches for ${fdsId}")
        } catch(e: Exception) {
            logger.logError("SyncCache: Could not get stored items from db $fdsId", null)
        }
    }

    override suspend fun syncCache(listOfCaches: List<Flexds<D>>): Result<Unit> {
        logger.log("""SyncCache: Db $fdsId has the following caches: ${setOfCaches.map { it.fdsId }.joinToString(", ")}""")
        logger.log("""SyncCache: Starting synchronization $fdsId with following caches: ${listOfCaches.map { it.fdsId }.joinToString(", ")}""")
        assert(listOfCaches.all { setOfCaches.contains(it) }) {
            """SyncCache: Attempt to sync the following caches: ${
                setOfCaches.map { it.fdsId }.joinToString(", ")
            }. But function syncCache was called with: '$listOfCaches'"""
        }
        val startSyncTime = FdsMetadata(System.currentTimeMillis().toString())
        deleteUnnecessaryItems()
        return try {
            val itemIds = listStoredIds().getOrThrow().shuffled()
            val cacheNotOk: MutableSet<Flexds<D>> = mutableSetOf()

            val results = mutableListOf<Result<D>>()

            // Iterate through each item and try to save it in caches
            for (itemId in itemIds) {
                try {
                    if (unmutable == true) {
                        val cachesWithoutItem = listOfCaches.filter { it.containsId(itemId).getOrNull() != true}
                        if (cachesWithoutItem.isNotEmpty()) {
                            val item = findById(itemId).getOrThrow()
                            for (cache in listOfCaches) {
                                try {
                                    results.add(cache.save(itemId, item))
                                } catch(e: Exception) {
                                    cacheNotOk.add(cache)
                                    logger.logError("SyncCache: Could not save ${dataClazz.simpleName} $itemId in cache ${cache.fdsId}", e)
                                    results.add(Result.failure(e))
                                }
                            }
                        }
                    } else {
                        val item = findById(itemId).getOrThrow()
                        for (cache in listOfCaches) {
                            try {
                                if (cache.findById(itemId).getOrNull() != item || item == null) results.add(cache.save(itemId, item))
                            } catch (e: Exception) {
                                cacheNotOk.add(cache)
                                logger.logError(
                                    "SyncCache: Could not save ${dataClazz.simpleName} $itemId in cache ${cache.fdsId}", e)
                                results.add(Result.failure(e))
                            }
                        }
                    }
                } catch (e: Exception) {
                    logger.logError("SyncCache: Could not retrieve ${dataClazz.simpleName} $itemId", e)
                    results.add(Result.failure(e))
                }
            }

            // Update the last sync time metadata for successful caches
            for (cache in listOfCaches) {
                if (cache !in cacheNotOk) {
                    try {
                        logger.log("SyncCache: Cache ${cache.fdsId} successfully synchronized.")
                        cache.saveMetadata(lastCacheSyncTimeMetaName, startSyncTime)
                    } catch (e: Exception) {
                        logger.logError("SyncCache: Could not save lastCacheSyncTime for cache ${cache.fdsId}", e)
                    }
                } else {
                    logger.logError("SyncCache: Cache ${cache.fdsId} could not be synchronized.", null)
                }
            }

            // Check if all results were successful
            if (results.all { it.isSuccess }) {
                logger.log("SyncCache: Successfully synced caches.")
                Result.success(Unit)
            } else {
                Result.failure(IllegalStateException("SyncCache: Could not sync cache: errors occurred during syncing"))
            }

        } catch (e: Exception) {
            logger.logError("SyncCache: Could not sync cache", e)
            Result.failure(e)
        }
    }
}
