package com.ajoinfinity.flexds.features.addcache

import com.ajoinfinity.flexds.Cacheable
import com.ajoinfinity.flexds.invokeWithCache
import com.ajoinfinity.flexds.main.Flexds
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import java.io.IOException

class AddCacheDecorator<D>(
    val fds: Flexds<D>,
    override val cache: Flexds<D>,
    override val fdsId: String = "${fds.fdsId}+Cache<${cache.fdsId}>",
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
    override val name: String
        get() = "Decorator(${fds.name}<${cache.name}>)"

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
            logger.logError("Failed to save to cache: $id")
        }
        return fds.save(id, data).also {
            if (it.isFailure) {
                logger.logError("Failed to save to data source: $id")
            }
        }
    }

    override suspend fun update(id: String, data: D): Result<D> {
        val cacheResult = cache.update(id, data)
        if (cacheResult.isFailure) {
            logger.logError("Failed to update cache: $id")
        }
        return fds.update(id, data).also {
            if (it.isFailure) {
                logger.logError("Failed to update data source: $id")
            }
        }
    }

    override suspend fun findById(id: String): Result<D> {
        addCacheDelegate.newRetrieval()

        // Fetch the local result
        val localResult = cache.findById(id)

        // Launch the remote lookup asynchronously, but don't wait for it immediately
        val remoteDeferred = coroutineScope {
            async {
                val remoteResult = fds.findById(id)
                if (remoteResult.isSuccess) {
                    try {
                        if (localResult.getOrNull() != remoteResult.getOrThrow()) {
                            cache.save(id, remoteResult.getOrThrow())
                        }
                    } catch (e: Exception) {
                        logger.logError("AddCacheDecorator: Could not save ${dataClazz.simpleName} $id in cache ${cache.name}")
                    }
                }
                remoteResult
            }
        }

        if (localResult.isSuccess) {
            addCacheDelegate.cacheHits++
            // Immediately return the local result without waiting for the remote lookup
            return localResult
        } else {
            addCacheDelegate.cacheMisses++
            // If the local result fails, wait for the remote lookup and return its result
            return remoteDeferred.await()
        }
    }

    override suspend fun delete(id: String): Result<String> {

        if (cache.containsId(id).getOrNull() ?: false) cache.delete(id)
        //if (cacheResult.isFailure) logger.logError("Failed to delete in cache: $id")

        return fds.delete(id).also {
            if (it.isFailure) {
                logger.logError("Failed to delete in data source: $id")
            }
        }
    }

    override suspend fun deleteAll(): Result<Unit> {
        return if ((listOf(fds.deleteAll(),cache.deleteAll()).all { it.isSuccess }))
            Result.success(Unit)
        else Result.failure(IOException("Could not deleteAll ${fds.name} and/or deleteAll in ${cache.name}"))
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
