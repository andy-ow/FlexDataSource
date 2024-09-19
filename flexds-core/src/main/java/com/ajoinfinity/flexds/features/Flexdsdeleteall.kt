package com.ajoinfinity.flexds.features

import com.ajoinfinity.flexds.exceptions.CompositeException

interface Flexdsdeleteall<D>: FlexdsListStoredIds<D>, FlexdsDelete<D> {
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