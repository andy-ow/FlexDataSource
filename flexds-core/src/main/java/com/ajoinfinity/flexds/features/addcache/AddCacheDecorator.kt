package com.ajoinfinity.flexds.features.addcache

import com.ajoinfinity.flexds.Flexds

class AddCacheDecorator<D>(
    val fds: Flexds<D>,
    val cache: Flexds<D>,
    ) : Flexds<D> {

        override val fdsId: String = "${fds.fdsId}<Cache: ${cache.fdsId}>"
        override val dataTypeName: String = fds.dataTypeName
        override suspend fun containsId(id: String): Result<Boolean> {
            return runCatching {
                val cacheResult = cache.containsId(id).getOrThrow()
                val withoutCacheResult = fds.containsId(id).getOrThrow()
                cacheResult || withoutCacheResult
            }
        }

        val mainDsName: String = fds.name
        val cacheName: String = cache.name
        override val name = "$mainDsName(Cache: $cacheName)"
        override val SHOULD_NOT_BE_USED_AS_CACHE = fds.SHOULD_NOT_BE_USED_AS_CACHE
        override fun showDataflow() = "${cache.showDataflow()}${fds.showDataflow()}"


        // Cache hit/miss statistics
        private var cacheHits: Int = 0
        private var cacheMisses: Int = 0
        private var totalRetrievals: Int = 0

        init {
            require(fds.dataTypeName == cache.dataTypeName) {
                "Datasource and cache must have the same datatype"
            }

            require(!cache.SHOULD_NOT_BE_USED_AS_CACHE) {
                "$name: Internal error: $cache is or contains a data source which should never act as a cache."
            }
        }


        override suspend fun save(id: String, data: D): Result<D> {
            val cacheResult = cache.save(id, data)
            if (cacheResult.isFailure) {
                logger.logError("$name: Failed to save to cache for $dataTypeName $id")
            }
            val remoteResult = fds.save(id, data)

            if (!remoteResult.isSuccess) {
                val error = "$name: Failed to save $dataTypeName"
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
                return Result.failure(IllegalArgumentException("$name: $dataTypeName name cannot be blank"))
            }

            val localResult = cache.findById(id)

            if (localResult.isSuccess) {
                cacheHits++
                //logger.log("$name: Cache hit for file: $fileName")
            } else {
                cacheMisses++
                //logger.log("$name: Cache miss for file: $fileName. Retrieving from remote.")
                val remoteResult = fds.findById(id)

                // If the remote retrieval is successful, cache the file locally
                if (remoteResult.isSuccess) {
                    cache.save(id, remoteResult.getOrNull()!!)
                } else {
                    logger.logError("$name: $dataTypeName '$id' not available here or in cache ${cache.name}.")
                }
                return remoteResult
            }

            return localResult
        }

        override suspend fun delete(id: String): Result<String> {
            val localResult = cache.delete(id)
            val remoteResult = fds.delete(id)
            if (localResult.isFailure) logger.logError("$name: Failed to delete $dataTypeName $id in cache ${cache.name}.")
            return if (remoteResult.isSuccess) {
                remoteResult
            } else {
                val message = "$name: Failed to delete $dataTypeName $id "
                logger.logError(message)
                Result.failure(Exception(message))
            }
        }

        override suspend fun listStoredIds(): Result<List<String>> {
            logger.logError("Maybe Cache should store this item list, since we always know if something is saved? Could be improved? We need to check if dscache has this information? What if cache was cleared?")
            return fds.listStoredIds()
            //return dsCache.listStoredIds()
        }

        override suspend fun getLastModificationTime(): Result<Long> {
            // this we can get from cache, even if cache was cleared?
            return fds.getLastModificationTime()
        }

        override suspend fun getNumberOfElements(): Result<Int> {
            // number of items
            logger.logError("Should be improved? Would be much better to have this info in cache.")
            return fds.getNumberOfElements()
        }

        override suspend fun update(id: String, data: D): Result<D> {
            return save(id, data)
        }

        // Print cache hit/miss stats every 100 file retrievals
        private fun printCacheStats() {
            val successRate: Int = (100 * cacheHits.toFloat() / totalRetrievals.toFloat()).toInt()
            logger.log("$name: Cache Stats: ${successRate}% success rate, $cacheHits hits, $cacheMisses misses after $totalRetrievals retrievals.")
            // Reset the counters after logging
            //cacheHits = 0
            //cacheMisses = 0
        }
    }
