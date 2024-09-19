package com.ajoinfinity.flexds.decorators

import com.ajoinfinity.flexds.FlexDataSource
import com.ajoinfinity.flexds.Logger

abstract class FlexDecorator<D>(protected val fds: FlexDataSource<D>): FlexDataSource<D> {
    override val SHOULD_NOT_BE_USED_AS_CACHE: Boolean
        get() = fds.SHOULD_NOT_BE_USED_AS_CACHE
    override val logger: Logger
        get() = fds.logger
    override val dataSourceId: String
        get() = fds.dataSourceId
    override val dsName: String
        get() = fds.dsName
    override val dataTypeName: String
        get() = fds.dataTypeName
    override suspend fun containsId(id: String): Result<Boolean> = fds.containsId(id)
    override suspend fun findById(id: String): Result<D> = fds.findById(id)
    override suspend fun delete(id: String): Result<Unit> = fds.delete(id)
    override suspend fun listStoredIds(): Result<List<String>> = fds.listStoredIds()
    override suspend fun getTimeLastModification(): Result<Long> = fds.getTimeLastModification()
    override suspend fun getNumberOfElements(): Result<Int> = fds.getNumberOfElements()
    override suspend fun update(id: String, data: D): Result<Unit> = update(id, data)
    override suspend fun save(id: String, data: D): Result<Unit> = save(id, data)
}