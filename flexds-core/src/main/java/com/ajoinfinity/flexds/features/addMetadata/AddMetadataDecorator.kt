package com.ajoinfinity.flexds.features.addMetadata

import com.ajoinfinity.flexds.main.Flexds

class AddMetadataDecorator<D>(
    fds: Flexds<D>,
    private val metadataFds: Flexds<String>  // Keep this as Flexds<String>
) : Flexds<D> by fds {

    // Convert from FdsMetadata to String when saving
    override suspend fun saveMetadata(id: String, data: FdsMetadata): Result<FdsMetadata> {
        // Save as String
        val result = metadataFds.save(id, data.value)
        return result.mapCatching { FdsMetadata(it) }
    }

    // Convert from String to FdsMetadata when retrieving
    override suspend fun findByIdMetadata(id: String): Result<FdsMetadata> {
        // Retrieve as String and convert to FdsMetadata
        val result = metadataFds.findById(id)
        println("findByIdMetadata: id: '$id' result: '$result'")
        return result.mapCatching { FdsMetadata(it) }
    }

    override suspend fun deleteMetadata(id: String): Result<String> {
        return metadataFds.delete(id)
    }
}