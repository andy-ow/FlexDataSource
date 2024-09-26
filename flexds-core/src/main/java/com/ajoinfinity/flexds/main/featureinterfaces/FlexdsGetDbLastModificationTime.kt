package com.ajoinfinity.flexds.main.featureinterfaces

import kotlinx.coroutines.flow.Flow

interface FlexdsGetDbLastModificationTime {

    suspend fun getLastModificationTime(): Result<Long> {
        throw NotImplementedError("Feature is not implemented. Please use GetDbLastModificationTimeDecorator")
    }

    fun observeDbLastModificationTime(): Flow<Long> {
        throw NotImplementedError("Feature is not implemented. Please use GetDbLastModificationTimeDecorator")
    }
}
