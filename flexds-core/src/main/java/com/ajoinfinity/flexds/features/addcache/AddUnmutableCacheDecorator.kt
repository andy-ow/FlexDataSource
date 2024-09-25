package com.ajoinfinity.flexds.features.addcache

import com.ajoinfinity.flexds.main.Flexds

class AddUnmutableCacheDecorator<D>(
    val fds: Flexds<D>,
    override val cache: Flexds<D>,
) : Flexds<D> by AddCacheDecorator(fds, cache) {

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
                    logger.logError("AddUnmutableCacheDecorator: Could not save ${dataClazz.simpleName} $id in cache ${cache.name}")
                }
            }
            remoteResult // Return the remote result, success or failure
        }
    }
}
