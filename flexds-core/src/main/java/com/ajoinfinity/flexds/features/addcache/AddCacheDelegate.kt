package com.ajoinfinity.flexds.features.addcache

import com.ajoinfinity.flexds.Flexds
import com.ajoinfinity.flexds.Logger

class AddCacheDelegate<D>(
    private val fds: Flexds<D>,
    private val cache: Flexds<D>
) {
    val logger: Logger = fds.logger

    // Cache hit/miss statistics
    internal var cacheHits: Int = 0
    internal var cacheMisses: Int = 0
    private var totalRetrievals: Int = 0

    internal fun printCacheStatsIfNecessary() {
        if (totalRetrievals % 5000 == 0) {
            val successRate = (100 * cacheHits.toFloat() / totalRetrievals.toFloat()).toInt()
            logger.log("Cache Stats: $successRate% success rate, $cacheHits hits, $cacheMisses misses after $totalRetrievals retrievals.")
        }
        totalRetrievals++
    }
}
