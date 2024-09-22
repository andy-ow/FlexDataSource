package com.ajoinfinity.flexds.features.liststoredids

import com.ajoinfinity.flexds.main.Flexds
import com.ajoinfinity.flexds.features.addMetadata.FdsMetadata
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ListStoredIdsDecorator<D>(
    val fds: Flexds<D>
) : Flexds<D> by fds {

    // Store the list of IDs in metadata
    private val idsListMetadataPath = "stored_ids_list_${fds.fdsId}"

    private fun verifyId(id: String): Exception? {
        if (id.contains(',')) return IllegalArgumentException("Comma ',' not allowed in id when using ListOfStoredIds feature")
        return null
    }
    // Retrieve the list of stored IDs from metadata
    private suspend fun getStoredIds(): List<String> {
        return fds.findByIdMetadata(idsListMetadataPath)
            .mapCatching { metadata -> metadata.value.split(",").filter { it.isNotEmpty() } }
            .getOrElse { emptyList() }
    }

    // Save the list of stored IDs as metadata
    private fun saveStoredIds(ids: List<String>) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val metadata = FdsMetadata(ids.joinToString(","))
                fds.saveMetadata(idsListMetadataPath, metadata)
            } catch (e: Exception) {
                logger.logError("Could not save metadata in $idsListMetadataPath", e)
            }
        }
    }

    // Add ID to the list when saving
    override suspend fun save(id: String, data: D): Result<D> {
        val idVerification = verifyId(id)
        if (idVerification != null) return Result.failure(idVerification)
        val result = fds.save(id, data)
        result.onSuccess {
            val ids = getStoredIds() + id
            saveStoredIds(ids.distinct()) // Save distinct IDs to metadata
        }
        return result
    }

    // Remove ID from the list when deleting
    override suspend fun delete(id: String): Result<String> {
        val result = fds.delete(id)
        result.onSuccess {
            val ids = getStoredIds().filter { it != id }
            saveStoredIds(ids)
        }
        return result
    }

    // List the currently stored IDs by fetching from metadata
    override suspend fun listStoredIds(): Result<List<String>> {
        return Result.success(getStoredIds())
    }

    // Optional override: containsId can refer to the stored list of IDs in metadata
    override suspend fun containsId(id: String): Result<Boolean> {
        val ids = getStoredIds()
        return Result.success(ids.contains(id))
    }

}
