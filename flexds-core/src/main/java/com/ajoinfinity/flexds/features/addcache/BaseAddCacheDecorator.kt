package com.ajoinfinity.flexds.features.addcache

import com.ajoinfinity.flexds.Flexds
import com.ajoinfinity.flexds.Logger

abstract class BaseAddCacheDecorator<D>(
    protected val fds: Flexds<D>,
    protected val secondaryFds: Flexds<D>,
    ): Flexds<D> {
    override val SHOULD_NOT_BE_USED_AS_CACHE: Boolean
        get() = fds.SHOULD_NOT_BE_USED_AS_CACHE
    override val logger: Logger
        get() = fds.logger
    override val fdsId: String
        get() = fds.fdsId
    override val name: String
        get() = fds.name
    override val dataTypeName: String
        get() = fds.dataTypeName

    override suspend fun listStoredIds(): Result<List<String>> = fds.listStoredIds()
    override suspend fun getLastModificationTime(): Result<Long> = fds.getLastModificationTime()
    override suspend fun getNumberOfElements(): Result<Int> = fds.getNumberOfElements()
    override suspend fun update(id: String, data: D): Result<D> = update(id, data)
    override suspend fun save(id: String, data: D): Result<D> = save(id, data)
}