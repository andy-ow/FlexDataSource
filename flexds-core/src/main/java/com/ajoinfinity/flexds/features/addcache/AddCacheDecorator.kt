package com.ajoinfinity.flexds.features.addcache

import com.ajoinfinity.flexds.Flexds

class AddCacheDecorator<D>(
    override val fds: Flexds<D>,
    override val cache: Flexds<D>,
    override val fdsId: String = "${fds.fdsId}+Cache<${cache.fdsId}>"
) : BaseAddCacheDecorator<D>(fds, cache) {

    private val addCacheDelegate = AddCacheDelegate(fds, cache)

    override fun showDataflow(): String {
        return " [${cache.showDataflow()} ${fds.showDataflow()}] "
    }
    override val name: String
        get() = "${fds.name}+Cache<${cache.name}>"

    override suspend fun containsId(id: String): Result<Boolean> {
        return try {
            val cacheResult = cache.containsId(id).getOrThrow()
            val withoutCacheResult = fds.containsId(id).getOrThrow()
            Result.success(cacheResult || withoutCacheResult)
        } catch (e: Exception) {
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
        addCacheDelegate.printCacheStatsIfNecessary()

        val localResult = cache.findById(id)
        return if (localResult.isSuccess) {
            addCacheDelegate.cacheHits++
            localResult
        } else {
            addCacheDelegate.cacheMisses++
            val remoteResult = fds.findById(id)
            if (remoteResult.isSuccess) {
                cache.save(id, remoteResult.getOrNull()!!)
            }
            remoteResult
        }
    }

    override suspend fun delete(id: String): Result<String> {
        val cacheResult = cache.delete(id)
        if (cacheResult.isFailure) logger.logError("Failed to delete in cache: $id")

        return fds.delete(id).also {
            if (it.isFailure) {
                logger.logError("Failed to delete in data source: $id")
            }
        }
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
