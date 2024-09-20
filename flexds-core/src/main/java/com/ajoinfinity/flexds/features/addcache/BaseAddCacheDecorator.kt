package com.ajoinfinity.flexds.features.addcache

import com.ajoinfinity.flexds.Flexds
import com.ajoinfinity.flexds.Logger
import com.ajoinfinity.flexds.features.FlexdsAddCache

abstract class BaseAddCacheDecorator<D>(
    open val fds: Flexds<D>,
    open val cache: Flexds<D>,
    ): FlexdsAddCache, Flexds<D> {
    override val SHOULD_NOT_BE_USED_AS_CACHE: Boolean
        get() = fds.SHOULD_NOT_BE_USED_AS_CACHE
    override val logger: Logger
        get() = fds.logger
    override val dataTypeName: String
        get() = fds.dataTypeName

    override suspend fun listStoredIds(): Result<List<String>> = fds.listStoredIds()
    override suspend fun getLastModificationTime(): Result<Long> = fds.getLastModificationTime()
    override suspend fun getNumberOfElements(): Result<Int> = fds.getNumberOfElements()
}