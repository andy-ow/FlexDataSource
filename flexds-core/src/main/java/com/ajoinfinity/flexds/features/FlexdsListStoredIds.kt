package com.ajoinfinity.flexds.features

import com.ajoinfinity.flexds.exceptions.CompositeException
import com.ajoinfinity.flexds.features.logger.FlexdsLogger

interface FlexdsListStoredIds<D>: FlexdsLogger {

    suspend fun listStoredIds(): Result<List<String>>

    suspend fun getNumberOfElements(): Result<Int> {
        return try {
            Result.success(listStoredIds().getOrThrow().size)
        } catch(e: Exception) {
            logger.logError("Could not get list of stored ids", e)
            Result.failure(e)
        }
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
