package com.ajoinfinity.flexds.features

import com.ajoinfinity.flexds.features.addMetadata.FdsMetadata

interface FlexdsAddMetadata {
    suspend fun saveMetadata(id: String, data: FdsMetadata): Result<FdsMetadata> {
        throw NotImplementedError("Feature is not implemented. Please use AddMetadataDecorator")
    }

    suspend fun findByIdMetadata(id: String): Result<FdsMetadata> {
        throw NotImplementedError("Feature is not implemented. Please use AddMetadataDecorator")
    }

    suspend fun deleteMetadata(id: String): Result<String> {
        throw NotImplementedError("Feature is not implemented. Please use AddMetadataDecorator")
    }
}