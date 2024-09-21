package com.ajoinfinity.flexds.features.addcache

import com.ajoinfinity.flexds.Flexds
import com.ajoinfinity.flexds.features.FlexdsAddCache
import java.io.IOException

class AddCacheDecorator<D>(
    val fds: Flexds<D>,
    override val cache: Flexds<D>,
    override val fdsId: String = "${fds.fdsId}+Cache<${cache.fdsId}>",
    private val addCacheDelegate: AddCacheDelegate<D> = AddCacheDelegate(fds, cache)
) : Flexds<D> by fds {

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
        return try {
            val localResult = cache.findById(id)
            if (localResult.isSuccess) {
                addCacheDelegate.cacheHits++
                localResult
            } else {
                addCacheDelegate.cacheMisses++
                val remoteResult = fds.findById(id)
                if (remoteResult.isSuccess) {
                    //cache.save(id, remoteResult.getOrNull()!!)
                }
                remoteResult
            }
        } catch (e: Exception) {
            logger.logError("AddCacheDecorator: findById: Could not find $dataTypeName $id", e)
            Result.failure(e)
        }

    }

    override suspend fun delete(id: String): Result<String> {

        val cacheResult = if (cache.containsId(id).getOrThrow()) cache.delete(id) else Result.success(Unit)
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
        else Result.failure(IOException("Could not deleteAll ${fds.name} and/or ${cache.name}"))
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
