package com.ajoinfinity.flexds.basedatasources

import com.ajoinfinity.flexds.Flexds
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

class FilesystemDS<D> (
    filesDir: File,
    override val fdsId: String,
    private val dataClass: D = "some string" as D,
    val serializer: KSerializer<D>? = null,
    override val SHOULD_NOT_BE_USED_AS_CACHE: Boolean = false,
    override val dataTypeName: String = "File",
) : Flexds<D> {

    private val json: Json = Json { prettyPrint = true }

    override val name: String = "FS<$fdsId>"

    // Always use a subdirectory relative to context.filesDir
    private val directory: File = File(filesDir, fdsId)

    private val mutex = Mutex()
    init {
        require(dataClass is String || dataClass is ByteArray || serializer != null) {
            "D must be either String, ByteArray, or Serializable"
        }
        // Ensure the directory exists or try to create it
        if (!directory.exists()) {
            val created = directory.mkdirs()
            if (!created) {
                throw IOException("Failed to create directory: ${directory.absolutePath}")
            }
        }
    }

    override suspend fun containsId(id: String): Result<Boolean> {
        return mutex.withLock {
            try {
                val file = File(directory, id)
                Result.success(file.exists())
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun save(id: String, data: D): Result<D> {
        //mutex.withLock {
           return try {
                val file = File(directory, id)
                when (data) {
                    is ByteArray -> file.writeBytes(data)
                    is String -> file.writeText(data)
                    else -> {
                        // Check if the serializer is provided for other types
                        serializer?.let {
                            val serializedData = json.encodeToString(it, data)
                            file.writeText(serializedData)
                        } ?: throw IllegalArgumentException("Unsupported type: ${data!!::class.java}")
                    }
                }
                Result.success(data)
            } catch (e: IOException) {
                val errorMsg = "Failed to save file with id '$id': ${e.message}"
                logger.logError(errorMsg, e)
                Result.failure(IOException(errorMsg, e))
            } catch (e: Exception) {
                logger.logError("Unexpected error in save for id '$id'", e)
                Result.failure(e)
            }
       // }
    }

    override suspend fun findById(id: String): Result<D> {
        return             try {               //mutex.withLock {
                val file = File(directory, id)
                if (file.exists()) {
                    val data: D = when (dataClass) {
                        ByteArray::class.java -> file.readBytes() as D
                        String::class.java -> file.readText() as D
                        else -> {
                            // Check if a serializer is provided for Serializable types
                            if (serializer != null) {
                                json.decodeFromString(serializer, file.readText()) as D
                            } else {
                                throw IllegalArgumentException("Unsupported type: ${dataClass!!::class.java}")
                            }
                        }
                    }
                    Result.success(data)
                } else {
                    val errorMsg = "File not found with id '$id'"
                    logger.logError(errorMsg)
                    Result.failure(FileNotFoundException(errorMsg))
                }
            } catch (e: IOException) {
                val errorMsg = "Failed to read file with id '$id': ${e.message}"
                logger.logError(errorMsg, e)
                Result.failure(IOException(errorMsg, e))
            } catch (e: Exception) {
                logger.logError("Unexpected error in findById for id '$id'", e)
                Result.failure(e)
            }

    }


    override suspend fun update(id: String, data: D): Result<D> {
        return save(id, data)
    }

    override suspend fun delete(id: String): Result<String> {
        mutex.withLock {
            return try {
                val file = File(directory, id)
                if (file.exists()) {
                    val deleted = file.delete()
                    if (deleted) {
                        Result.success(id)
                    } else {
                        val errorMsg = "Failed to delete file with id '$id'"
                        logger.logError(errorMsg)
                        Result.failure(IOException(errorMsg))
                    }
                } else {
                    val errorMsg = "File not found with id '$id'"
                    logger.logError(errorMsg)
                    Result.failure(FileNotFoundException(errorMsg))
                }
            } catch (e: Exception) {
                logger.logError("Unexpected error in delete for id '$id'", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun listStoredIds(): Result<List<String>> {
        return mutex.withLock {
            try {
                val files = directory.listFiles()?.map { it.name } ?: emptyList()
                Result.success(files)
            } catch (e: Exception) {
                logger.logError("Failed to list stored ids", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun getLastModificationTime(): Result<Long> {
        return mutex.withLock {
            try {
                val lastModifiedTime = directory.listFiles()
                    ?.map { it.lastModified() }
                    ?.maxOrNull() ?: 0L
                Result.success(lastModifiedTime)
            } catch (e: Exception) {
                logger.logError("Failed to get last modification time", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun getNumberOfElements(): Result<Int> {
        return mutex.withLock {
            try {
                val fileCount = directory.listFiles()?.size ?: 0
                Result.success(fileCount)
            } catch (e: Exception) {
                logger.logError("Failed to get size", e)
                Result.failure(e)
            }
        }
    }
}
