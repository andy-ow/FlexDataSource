package com.ajoinfinity.flexds

import com.ajoinfinity.flexds.decorators.DataSourceToCacheDecorator
import com.ajoinfinity.poleconyksiegowy.data.datasource.decorators.AddingCacheToDataSource


interface DataSource<D> : AbstractDS<D> {
    override val SHOULD_NOT_BE_USED_AS_CACHE: Boolean
    val logger: Logger // = DefaultLogger()

    fun addCache(
        dataSource: DataSource<D>,
        cacheSizeInMb: Long,
    ): DataSourceWithCache<D> {
        val cache = DataSourceToCacheDecorator<D>(
            cacheSizeInMB = cacheSizeInMb,
            dataSource = dataSource,
            logger = logger,
        )
        return AddingCacheToDataSource<D>(
            dsCache = cache,
            dsWithoutCache = this
        )
    }
}

