package com.ajoinfinity.flexds.features.addcache

import com.ajoinfinity.flexds.main.Flexds
import com.ajoinfinity.flexds.main.logger.Logger
import com.ajoinfinity.flexds.main.featureinterfaces.FlexdsAddCache

class AddCacheDelegate<D>(
    private val fds: Flexds<D>,
    override val cache: Flexds<D>
): FlexdsAddCache<D> {
    val logger: Logger = fds.logger

    // Cache hit/miss statistics
    internal var cacheHits: Int = 0
    internal var cacheMisses: Int = 0
    private var totalRetrievals: Int = 0

    internal fun newRetrieval() {
        totalRetrievals += 1
    }

    override fun clearCacheStats() {
        cacheHits = 0; cacheMisses = 0; totalRetrievals = 0;
    }

    override suspend fun displayCacheStats() {
            val successRate = (100 * cacheHits.toFloat() / totalRetrievals.toFloat()).toInt()
            logger.log("Cache Stats: $successRate% success rate, $cacheHits hits, $cacheMisses misses after $totalRetrievals retrievals.")
            println("cache number of items: ${cache.getNumberOfElements().getOrNull()}")
            println("cache current size: ${cache.getFlexdsSize()}")
    }
}
