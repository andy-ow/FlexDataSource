package com.ajoinfinity.flexds.features.addcache

import com.ajoinfinity.flexds.main.Flexds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException

class AddCacheDecorator<D>(
    val fds: Flexds<D>,
    override val cache: Flexds<D>,
    private val addCacheDelegate: AddCacheDelegate<D> = AddCacheDelegate(fds, cache)
) : Flexds<D> by fds {

    override val setOfCaches: Set<Flexds<D>>
        get() = fds.setOfCaches + setOf(cache) + cache.setOfCaches

    override fun clearCacheStats() {
        addCacheDelegate.clearCacheStats()
    }

    override suspend fun displayCacheStats() {
        addCacheDelegate.displayCacheStats()
    }

    override fun showDataflow(): String {
        return " [${cache.showDataflow()} --> ${fds.showDataflow()}] "
    }

    override suspend fun containsId(id: String): Result<Boolean> {
        return try {
            // Try checking the cache first
            val cacheResult = cache.containsId(id)
            if (cacheResult.getOrDefault(false)) {
                return Result.success(true)  // If the ID is found in the cache, return true
            }

            // If not found in cache or cache fails, check the primary data source
            val fdsResult = fds.containsId(id)
            Result.success(fdsResult.getOrThrow())  // Return the result from the primary data source
        } catch (e: Exception) {
            // If any exception occurs, log it and return failure
            logger.logError("Error checking id in cache or data source", e)
            Result.failure(e)
        }
    }

    override suspend fun save(id: String, data: D): Result<D> {
        val cacheResult = cache.save(id, data)
        if (cacheResult.isFailure) {
            logger.logError("Failed to save to cache: $id", null)
        }
        return fds.save(id, data).also {
            if (it.isFailure) {
                logger.logError("Failed to save to data source: $id", null)
            }
        }
    }

    override suspend fun update(id: String, data: D): Result<D> {
        val cacheResult = cache.update(id, data)
        if (cacheResult.isFailure) {
            logger.logError("Failed to update cache: $id", null)
        }
        return fds.update(id, data).also {
            if (it.isFailure) {
                logger.logError("Failed to update data source: $id", null)
            }
        }
    }

    // Scope for background operations
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Map to keep track of in-progress operations per ID
    private val inProgressOperations = mutableMapOf<String, Deferred<Result<D>>>()

    // Mutex to synchronize access to the inProgressOperations map
    private val operationsMutex = Mutex()

    override suspend fun findById(id: String): Result<D> {
        // First, check the cache
        val localResult = cache.findById(id)
        return if (localResult.isSuccess) {
            // If the cache has the value, return it
            localResult
        } else {
            // Otherwise, fetch from the remote data source
            val remoteResult = fds.findById(id)
            if (remoteResult.isSuccess) {
                // If found remotely, save it in the cache and return it
                try {
                    cache.save(id, remoteResult.getOrThrow())
                } catch (e: Exception) {
                    logger.logError("AddCacheDecorator: Could not save ${dataClazz.simpleName} $id in cache ${cache.fdsId}", null)
                }
            }
            remoteResult
        }
    }

    private fun fetchAndUpdateCacheInBackground(id: String, localData: D?) {
        scope.launch {
            val remoteResult = fds.findById(id)
            if (remoteResult.isSuccess) {
                val remoteData = remoteResult.getOrThrow()
                if (localData != remoteData) {
                    try {
                        cache.save(id, remoteData)
                    } catch (e: Exception) {
                        logger.logError(
                            "AddCacheDecorator: Could not save ${dataClazz.simpleName} $id in cache ${cache.fdsId}",
                            e
                        )
                    }
                }
            }
        }
    }

    override suspend fun delete(id: String): Result<String> {

        if (cache.containsId(id).getOrNull() ?: false) cache.delete(id)
        //if (cacheResult.isFailure) logger.logError("Failed to delete in cache: $id")

        return fds.delete(id).also {
            if (it.isFailure) {
                logger.logError("Failed to delete in data source: $id", null)
            }
        }
    }

    override suspend fun deleteAll(): Result<Unit> {
        return if ((listOf(fds.deleteAll(),cache.deleteAll()).all { it.isSuccess }))
            Result.success(Unit)
        else Result.failure(IOException("Could not deleteAll ${fds.fdsId} and/or deleteAll in ${cache.fdsId}"))
    }

    override suspend fun listStoredIds(): Result<List<String>> {
        // Optionally, you can enhance this logic using the cache as needed
        return fds.listStoredIds()
    }

    override suspend fun getLastModificationTime(): Result<Long> {
        return fds.getLastModificationTime()  // Can also be enhanced to use the cache if needed
    }

    override suspend fun getNumberOfElements(): Result<Int> {
        return fds.getNumberOfElements()
    }

}
