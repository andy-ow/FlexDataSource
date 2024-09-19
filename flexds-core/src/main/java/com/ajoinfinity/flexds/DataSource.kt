package com.ajoinfinity.flexds

import com.ajoinfinity.flexds.datasources.DataSourceWithCache
import com.ajoinfinity.flexds.decorators.DsToCacheDecorator
import com.ajoinfinity.flexds.decorators.DsWithCacheDecorator
import com.ajoinfinity.flexds.exceptions.CompositeException


interface DataSource<D> : SharedFeatures<D> {
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

    override suspend fun deleteAll(): Result<Unit> {
        val idsResult = listStoredIds()

        if (idsResult.isFailure) {
            return Result.failure(idsResult.exceptionOrNull()!!)
        }

        val errors = mutableListOf<Throwable>()

        idsResult.getOrThrow().forEach { item ->
            val deleteResult = delete(item)
            if (deleteResult.isFailure) {
                errors.add(deleteResult.exceptionOrNull()!!)
            }
        }

        return if (errors.isEmpty()) {
            Result.success(Unit)
        } else {
            Result.failure(CompositeException(errors))
        }
    }

}

