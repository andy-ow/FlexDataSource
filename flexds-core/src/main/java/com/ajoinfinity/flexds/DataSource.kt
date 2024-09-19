package com.ajoinfinity.flexds

import com.ajoinfinity.flexds.datasources.DataSourceWithCache
import com.ajoinfinity.flexds.decorators.DsToCacheDecorator
import com.ajoinfinity.flexds.decorators.DsWithCacheDecorator


interface DataSource<D> : Flexds<D> {
    override val SHOULD_NOT_BE_USED_AS_CACHE: Boolean
    override val logger: Logger // = DefaultLogger()

    fun addCache(
        dataSource: DataSource<D>,
        cacheSizeInMb: Long,
        getSize: ((D) -> Long)? = null,
    ): DataSourceWithCache<D> {
        val cache = DsToCacheDecorator<D>(
            cacheSizeInMB = cacheSizeInMb,
            dataSource = dataSource,
            getSize = getSize,
            logger = logger,
        )
        return DsWithCacheDecorator<D>(
            dsCache = cache,
            dsWithoutCache = this
        )
    }

}

