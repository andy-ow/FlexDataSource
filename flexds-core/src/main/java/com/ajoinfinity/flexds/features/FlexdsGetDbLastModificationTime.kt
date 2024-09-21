package com.ajoinfinity.flexds.features

interface FlexdsGetDbLastModificationTime {

    suspend fun getLastModificationTime(): Result<Long> {
        throw NotImplementedError("Feature is not implemented. Please use GetDbLastModificationTimeDecorator")
    }

    fun getDbLastModificationTimeMetadataPath(): String {
        throw NotImplementedError("Feature is not implemented. Please use GetDbLastModificationTimeDecorator")
    }
}
