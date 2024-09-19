package com.ajoinfinity.flexds

import com.ajoinfinity.flexds.exceptions.CompositeException
import com.ajoinfinity.flexds.features.fdssize.FlexDataSourceSize


interface FlexDataSource<D> : FlexDataSourceSize {
    val SHOULD_NOT_BE_USED_AS_CACHE: Boolean
    abstract val dataSourceId: String
    abstract val dsName: String
    abstract val dataTypeName: String // name of datatype D, which will be shown in logs or errors, maybe visible to the user, for example 'File' or 'Node'
    val logger: Logger
        get() = FlexDataSourceManager.logger
    abstract suspend fun containsId(id: String): Result<Boolean>
    abstract suspend fun findById(id: String): Result<D>
    abstract suspend fun save(id: String, data: D): Result<Unit>
    abstract suspend fun update(id: String, data: D): Result<Unit>
    abstract suspend fun delete(id: String): Result<Unit>
    abstract suspend fun listStoredIds(): Result<List<String>>
    abstract suspend fun getTimeLastModification(): Result<Long>
    abstract suspend fun getNumberOfElements(): Result<Int>
    fun showDataflow(): String {
        return " --> $dsName"
    }

    suspend fun deleteAll(): Result<Unit> {
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

