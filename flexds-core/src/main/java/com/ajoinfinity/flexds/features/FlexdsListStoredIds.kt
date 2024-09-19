package com.ajoinfinity.flexds.features

import com.ajoinfinity.flexds.Flexds
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
}
