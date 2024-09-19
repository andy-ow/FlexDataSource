package com.ajoinfinity.flexds.basedatasources

import com.ajoinfinity.flexds.DataSource
import com.ajoinfinity.flexds.FlexDataSourceManager
import com.ajoinfinity.flexds.Logger
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.FileNotFoundException


class MemoryDS<D> constructor(
    override val fdsId: String,
    override val dataTypeName: String = "Item",
    override val SHOULD_NOT_BE_USED_AS_CACHE: Boolean = false,
    override val logger: Logger = FlexDataSourceManager.defaultLogger
) : DataSource<D> {

    override val name: String = "MemoryStorage<$fdsId>"

    // Memory storage to hold data
    private val memoryStore: MutableMap<String, Pair<D, Long>> = mutableMapOf()


    // Mutex for thread safety
    private val mutex = Mutex()

    // Time utility for tracking when data was last used
    private fun getCurrentTime() = System.currentTimeMillis()

    override suspend fun containsId(id: String): Result<Boolean> {
        if (id.isBlank()) {
            return Result.failure(IllegalArgumentException("$name: ID cannot be blank"))
        }
        return mutex.withLock {
            try {
                Result.success(memoryStore.containsKey(id))
            } catch (e: Exception) {
                logger.logError("$name: Error checking if ID exists: $id", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun save(id: String, data: D): Result<Unit> {
        if (id.isBlank()) {
            return Result.failure(IllegalArgumentException("$name: ID cannot be blank"))
        }
        return mutex.withLock {
            try {
                memoryStore[id] = data to getCurrentTime()
                Result.success(Unit)
            } catch (e: Exception) {
                logger.logError("$name: Error saving data with ID: $id", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun findById(id: String): Result<D> {
        if (id.isBlank()) {
            return Result.failure(IllegalArgumentException("$name: ID cannot be blank"))
        }
        return mutex.withLock {
            try {
                val dataEntry = memoryStore[id]
                if (dataEntry != null) {
                    // Update last access time
                    memoryStore[id] = dataEntry.first to getCurrentTime()
                    Result.success(dataEntry.first)
                } else {
                    val errorMsg = "$name: $dataTypeName not found: $id"
                    logger.logError(errorMsg)
                    Result.failure(FileNotFoundException(errorMsg))
                }
            } catch (e: Exception) {
                logger.logError("$name: Error finding data with ID: $id", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun update(id: String, data: D): Result<Unit> {
        try {
            save(id, data)
        } catch (e: Exception) {
        logger.logError("$name: Error while updating", e)
        Result.failure(e)
    }
        return Result.success(Unit)
    }

    override suspend fun delete(id: String): Result<Unit> {
        if (id.isBlank()) {
            return Result.failure(IllegalArgumentException("$name: ID cannot be blank"))
        }
        return mutex.withLock {
            try {
                if (memoryStore.remove(id) != null) {
                    Result.success(Unit)
                } else {
                    val errorMsg = "$name: $dataTypeName not found: $id"
                    logger.logError(errorMsg)
                    Result.failure(FileNotFoundException(errorMsg))
                }
            } catch (e: Exception) {
                logger.logError("$name: Error deleting data with ID: $id", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun listStoredIds(): Result<List<String>> {
        return mutex.withLock {
            try {
                Result.success(memoryStore.keys.toList())
            } catch (e: Exception) {
                logger.logError("$name: Error listing stored IDs", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun getTimeLastModification(): Result<Long> {
        return mutex.withLock {
            try {
                val lastModifiedTime = memoryStore.values.map { it.second }.maxOrNull() ?: 0L
                Result.success(lastModifiedTime)
            } catch (e: Exception) {
                logger.logError("$name: Error getting last modification time", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun getNumberOfElements(): Result<Int> {
        return mutex.withLock {
            try {
                Result.success(memoryStore.size)
            } catch (e: Exception) {
                logger.logError("$name: Error getting size", e)
                Result.failure(e)
            }
        }
    }

    companion object {

        // A map to store singletons of MemoryDS keyed by dataSourceId
        private val memoryDataSourceCache = mutableMapOf<String, DataSource<*>>()

        @Suppress("UNCHECKED_CAST")
        fun <D> createMemoryDataSource(
            dataSourceId: String,
            dataTypeName: String = "Item",
            SHOULD_NOT_BE_USED_AS_CACHE: Boolean = false,
            logger: Logger = FlexDataSourceManager.defaultLogger
        ): DataSource<D> {
            // Check if an instance for the given dataSourceId already exists
            val existingDataSource = memoryDataSourceCache[dataSourceId]

            if (existingDataSource != null) {
                // If the instance exists, ensure that all other parameters match
                val memoryDS = existingDataSource as MemoryDS<D> // Safe cast, as we store only MemoryDS

                if (memoryDS.dataTypeName != dataTypeName ||
                    memoryDS.SHOULD_NOT_BE_USED_AS_CACHE != SHOULD_NOT_BE_USED_AS_CACHE ||
                    memoryDS.logger != logger) {

                    throw IllegalStateException("A MemoryDS with dataSourceId '$dataSourceId' already exists but the provided parameters do not match the existing instance.")
                }

                // If all parameters match, return the existing instance
                return memoryDS
            }

            // If no instance exists, create a new one and store it in the map
            val newDataSource = MemoryDS<D>(
                fdsId = dataSourceId,
                dataTypeName = dataTypeName,
                SHOULD_NOT_BE_USED_AS_CACHE = SHOULD_NOT_BE_USED_AS_CACHE,
                logger = logger
            )
            memoryDataSourceCache[dataSourceId] = newDataSource

            return newDataSource
        }
    }
}
