package com.ajoinfinity.flexds.basedatasources

import com.ajoinfinity.flexds.main.Flexds
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.FileNotFoundException


class MemoryDS<D> constructor(
    override val fdsId: String,
    override val dataClazz: Class<D>,
    override val SHOULD_NOT_BE_USED_AS_CACHE: Boolean = false,
    override val unmutable: Boolean,
) : Flexds<D> {

    // Memory storage to hold data
    private val memoryStore: MutableMap<String, Pair<D, Long>> = mutableMapOf()


    // Mutex for thread safety
    private val mutex = Mutex()

    // Time utility for tracking when data was last used
    private fun getCurrentTime() = System.currentTimeMillis()

    override suspend fun containsId(id: String): Result<Boolean> {
        if (id.isBlank()) {
            return Result.failure(IllegalArgumentException("$fdsId: ID cannot be blank"))
        }
        return mutex.withLock {
            try {
                Result.success(memoryStore.containsKey(id))
            } catch (e: Exception) {
                logger.logError("$fdsId: Error checking if ID exists: $id", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun save(id: String, data: D): Result<D> {
        //logger.log("Real MemoryDS-$fdsId: Saving id $id")
        if (data != null && data!!::class.java != dataClazz) {
            throw IllegalArgumentException("Error: data class should be '${dataClazz.name}' but is '${data!!::class.java.name}'")
        }
        if (id.isBlank()) {
            return Result.failure(IllegalArgumentException("$fdsId: ID cannot be blank"))
        }
        mutex.withLock {
            return try {
                memoryStore[id] = data to getCurrentTime()
                Result.success(data)
            } catch (e: Exception) {
                logger.logError("$fdsId: Error saving data with ID: '$id'", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun findById(id: String): Result<D> {
        if (id.isBlank()) {
            return Result.failure(IllegalArgumentException("$fdsId: ID cannot be blank"))
        }
        return mutex.withLock {
            try {
                val dataEntry = memoryStore[id]
                if (dataEntry != null) {
                    // Update last access time
                    memoryStore[id] = dataEntry.first to getCurrentTime()
                    Result.success(dataEntry.first)
                } else {
                    val errorMsg = "$fdsId: ${dataClazz.simpleName} not found: '$id'"
                    //logger.logError(errorMsg)
                    Result.failure(IllegalArgumentException(errorMsg))
                }
            } catch (e: Exception) {
                //logger.logError("$name: Error finding data with ID: $id", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun update(id: String, data: D): Result<D> {
        try {
            save(id, data)
        } catch (e: Exception) {
        logger.logError("$fdsId: Error while updating", e)
        Result.failure(e)
    }
        return Result.success(data)
    }

    override suspend fun delete(id: String): Result<String> {
        if (id.isBlank()) {
            return Result.failure(IllegalArgumentException("$fdsId: ID cannot be blank"))
        }
        mutex.withLock {
            return try {
                if (memoryStore.remove(id) != null) {
                    Result.success(id)
                } else {
                    val errorMsg = "$fdsId: ${dataClazz.simpleName} not found: '$id'"
                    //logger.logError(errorMsg)
                    Result.failure(FileNotFoundException(errorMsg))
                }
            } catch (e: Exception) {
                logger.logError("$fdsId: Error deleting data with ID: '$id'", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun deleteAll(): Result<Unit> {
        return mutex.withLock {
            memoryStore.clear()
            Result.success(Unit)
        }
    }

    override suspend fun listStoredIds(): Result<List<String>> {
        return mutex.withLock {
            try {
                Result.success(memoryStore.keys.toList())
            } catch (e: Exception) {
                logger.logError("$fdsId: Error listing stored IDs", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun getLastModificationTime(): Result<Long> {
        return mutex.withLock {
            try {
                val lastModifiedTime = memoryStore.values.map { it.second }.maxOrNull() ?: 0L
                Result.success(lastModifiedTime)
            } catch (e: Exception) {
                logger.logError("$fdsId: Error getting last modification time", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun getNumberOfElements(): Result<Int> {
        return mutex.withLock {
            try {
                Result.success(memoryStore.size)
            } catch (e: Exception) {
                logger.logError("$fdsId: Error getting size", e)
                Result.failure(e)
            }
        }
    }
}
