package com.ajoinfinity.flexds.basedatasources

import com.ajoinfinity.flexds.Flexds
import java.io.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.KSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class IndexedFileFilesystemDS<D>(
    filesDir: File,
    override val fdsId: String,
    private val dataClass: D = "some string" as D,
    val serializer: KSerializer<D>,  // Serializer for the data
    override val SHOULD_NOT_BE_USED_AS_CACHE: Boolean = false,
    override val dataTypeName: String = "File",
) : Flexds<D> {

    private val json: Json = Json { prettyPrint = true }
    override val name: String = "IndexedFileStorage<$fdsId>"

    // File paths
    private val dataFile: File = File(filesDir, "$fdsId.data")
    private val indexFile: File = File(filesDir, "$fdsId.index")

    private val mutex = Mutex()

    // In-memory index for faster access
    private val indexMap = mutableMapOf<String, Pair<Long, Int>>()  // ID -> (offset, length)

    init {
        // Ensure the directory exists or try to create it
        if (!filesDir.exists()) {
            val created = filesDir.mkdirs()
            if (!created) {
                throw IOException("Failed to create directory: ${filesDir.absolutePath}")
            }
        }
        // Ensure that the data file and index file exist
        if (!dataFile.exists()) {
            dataFile.createNewFile()
        }
        if (!indexFile.exists()) {
            indexFile.createNewFile()
        }
        // Load the index into memory
        loadIndex()
    }

    private fun loadIndex() {
        if (indexFile.exists() && indexFile.length() > 0) {
            indexFile.bufferedReader().useLines { lines ->
                lines.forEach { line ->
                    val parts = line.split(",")
                    if (parts.size == 3) {
                        val id = parts[0]
                        val offset = parts[1].toLongOrNull()
                        val length = parts[2].toIntOrNull()
                        if (offset != null && length != null) {
                            indexMap[id] = Pair(offset, length)
                        }
                    }
                }
            }
        }
    }

    private fun updateIndex(id: String, offset: Long, length: Int) {
        indexMap[id] = Pair(offset, length)
        indexFile.appendText("$id,$offset,$length\n")
    }

    override suspend fun containsId(id: String): Result<Boolean> {
        return mutex.withLock {
            Result.success(indexMap.containsKey(id))
        }
    }

    override suspend fun save(id: String, data: D): Result<D> {
        return mutex.withLock {
            try {
                val serializedData = json.encodeToString(serializer, data)
                val dataBytes = serializedData.toByteArray()

                // Write data to the end of the data file
                RandomAccessFile(dataFile, "rw").use { raf ->
                    raf.seek(raf.length())  // Move to the end of the file
                    val offset = raf.filePointer
                    raf.write(dataBytes)
                    val length = dataBytes.size

                    // Update the index
                    updateIndex(id, offset, length)
                }
                Result.success(data)
            } catch (e: Exception) {
                Result.failure(IOException("Failed to save data: ${e.message}", e))
            }
        }
    }

    override suspend fun findById(id: String): Result<D> {
        return mutex.withLock {
            val (offset, length) = indexMap[id] ?: return@withLock Result.failure(
                FileNotFoundException("Data with id '$id' not found")
            )

            return@withLock try {
                val dataBytes = ByteArray(length)
                RandomAccessFile(dataFile, "r").use { raf ->
                    raf.seek(offset)  // Move to the correct position
                    raf.readFully(dataBytes)
                }
                val data = json.decodeFromString(serializer, String(dataBytes))
                Result.success(data)
            } catch (e: Exception) {
                Result.failure(IOException("Failed to read data: ${e.message}", e))
            }
        }
    }

    override suspend fun update(id: String, data: D): Result<D> {
        return save(id, data)  // Same logic as save
    }

    override suspend fun delete(id: String): Result<String> {
        return mutex.withLock {
            if (indexMap.containsKey(id)) {
                indexMap.remove(id)
                // Rewrite the index without the deleted entry
                indexFile.writeText("")
                indexMap.forEach { (key, value) ->
                    indexFile.appendText("$key,${value.first},${value.second}\n")
                }
                Result.success(id)
            } else {
                Result.failure(FileNotFoundException("Data with id '$id' not found"))
            }
        }
    }

    override suspend fun listStoredIds(): Result<List<String>> {
        return mutex.withLock {
            Result.success(indexMap.keys.toList())
        }
    }

    override suspend fun getLastModificationTime(): Result<Long> {
        return mutex.withLock {
            Result.success(dataFile.lastModified())
        }
    }

    override suspend fun getNumberOfElements(): Result<Int> {
        return mutex.withLock {
            Result.success(indexMap.size)
        }
    }
}
