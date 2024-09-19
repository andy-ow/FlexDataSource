package com.ajoinfinity.flexds.decorators

import com.ajoinfinity.flexds.DataSource
import com.ajoinfinity.flexds.Logger

abstract class DsDecorator<D>(protected val ds: DataSource<D>): DataSource<D> {
    override val SHOULD_NOT_BE_USED_AS_CACHE: Boolean
        get() = ds.SHOULD_NOT_BE_USED_AS_CACHE
    override val logger: Logger
        get() = ds.logger
    override val dataSourceId: String
        get() = ds.dataSourceId
    override val dsName: String
        get() = TODO("Not yet implemented")
    override val dataTypeName: String
        get() = TODO("Not yet implemented")

    override suspend fun containsId(id: String): Result<Boolean> {
        TODO("Not yet implemented")
    }

    override suspend fun findById(id: String): Result<D> {
        TODO("Not yet implemented")
    }

    override suspend fun delete(id: String): Result<Unit> {
        TODO("Not yet implemented")
    }

    override suspend fun listStoredIds(): Result<List<String>> {
        TODO("Not yet implemented")
    }

    override suspend fun getTimeLastModification(): Result<Long> {
        TODO("Not yet implemented")
    }

    override suspend fun getNumberOfElements(): Result<Int> {
        TODO("Not yet implemented")
    }

    override suspend fun update(id: String, data: D): Result<Unit> {
        TODO("Not yet implemented")
    }

    override suspend fun save(id: String, data: D): Result<Unit> {
        TODO("Not yet implemented")
    }
}