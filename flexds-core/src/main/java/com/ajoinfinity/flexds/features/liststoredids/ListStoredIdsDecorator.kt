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

    override suspend fun getNumberOfElements(): Result<Int> {
        return try {
            Result.success(listStoredIds().getOrThrow().size)
        } catch(e: Exception) {
            logger.logError("Could not get list of stored ids", e)
            Result.failure(e)
        }
    }

    // Save the list of stored IDs as metadata
    private suspend fun saveStoredIds(ids: List<String>) {
            try {
                val metadata = FdsMetadata(ids.joinToString(","))
                fds.saveMetadata(idsListMetadataPath, metadata)
            } catch (e: Exception) {
                logger.logError("Could not save metadata in $idsListMetadataPath", e)
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
        val ids = getStoredIds().filter { it != id }
        saveStoredIds(ids)
        result.onFailure {
            logger.logError("ListStoredIdsDecorator: Error while saving ${id}, trying again.", null)
            fds.delete(id).onFailure {
                logger.logError("ListStoredIdsDecorator: Error while saving ${id}, trying again.", null)
                fds.delete(id)
            }
        }
        return result
    }

    override suspend fun deleteAll(): Result<Unit> {
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
            Result.failure(IllegalStateException("Could not delete all items because of following errors: $errors"))
        }
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
