package com.ajoinfinity.flexds.basedatasources

import com.ajoinfinity.flexds.DataSource
import com.ajoinfinity.flexds.Logger
import com.ajoinfinity.flexds.FlexDataSourceManager
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseStorageDS<D> @Inject constructor(
    override val dataSourceId: String,
    firebaseStorage: FirebaseStorage,
    override val logger: Logger = FlexDataSourceManager.defaultLogger
) : DataSource<D> {

    override val dsName = "FirebaseStorage"
    override val dataTypeName = "File"
    override val SHOULD_NOT_BE_USED_AS_CACHE = true

    private val firebaseStorageRoot: StorageReference = firebaseStorage.reference.child(dataSourceId)

    // Check if the file with the given id exists in Firebase Storage
    override suspend fun containsId(id: String): Result<Boolean> {
        return try {
            val storageRef = firebaseStorageRoot.child(id)
            storageRef.metadata.await()
            Result.success(true)
        } catch (e: Exception) {
            if (e is StorageException && e.errorCode == StorageException.ERROR_OBJECT_NOT_FOUND) {
                Result.success(false)
            } else {
                logger.logError("Failed to check containsId for $id", e)
                Result.failure(e)
            }
        }
    }

    // Save the data (generic) to Firebase Storage under the given id
    override suspend fun save(id: String, data: D): Result<Unit> {
        return try {
            val storageRef = firebaseStorageRoot.child(id)
            when (data) {
                is ByteArray -> storageRef.putBytes(data).await()
                is InputStream -> storageRef.putStream(data).await()
                is File -> storageRef.putFile(android.net.Uri.fromFile(data)).await()
                else -> throw IllegalArgumentException("Unsupported type: ${data!!::class.java}")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            logger.logError("Failed to save data for $id", e)
            Result.failure(e)
        }
    }

    // Retrieve the data (generic) from Firebase Storage with the given id
    override suspend fun findById(id: String): Result<D> {
        return try {
            val storageRef = firebaseStorageRoot.child(id)
            val data: D = when (dataTypeName) {
                "ByteArray" -> storageRef.getBytes(Long.MAX_VALUE).await() as D
                "InputStream" -> storageRef.stream.await().stream as D
                "File" -> {
                    val localFile = File.createTempFile("tmp_$id", null)
                    storageRef.getFile(localFile).await()
                    localFile as D
                }
                else -> throw IllegalArgumentException("Unsupported type for retrieval: $dataTypeName")
            }
            Result.success(data)
        } catch (e: Exception) {
            logger.logError("Failed to retrieve data for $id", e)
            Result.failure(e)
        }
    }

    // Delete the file with the given id from Firebase Storage
    override suspend fun delete(id: String): Result<Unit> {
        return try {
            val storageRef = firebaseStorageRoot.child(id)
            storageRef.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            logger.logError("Failed to delete data for $id", e)
            Result.failure(e)
        }
    }

    // List all stored file IDs in Firebase Storage for the current dataSourceId
    override suspend fun listStoredIds(): Result<List<String>> {
        return try {
            val listResult = firebaseStorageRoot.listAll().await()
            val fileNames = listResult.items.map { it.name }
            Result.success(fileNames)
        } catch (e: Exception) {
            logger.logError("Failed to list stored IDs", e)
            Result.failure(e)
        }
    }

    // Get the time of the last modification (based on metadata) of any file in this data source
    override suspend fun getTimeLastModification(): Result<Long> {
        return try {
            val listResult = firebaseStorageRoot.listAll().await()
            val lastModified = listResult.items
                .mapNotNull { it.metadata.await().updatedTimeMillis }
                .maxOrNull() ?: 0L
            Result.success(lastModified)
        } catch (e: Exception) {
            logger.logError("Failed to get last modification time", e)
            Result.failure(e)
        }
    }

    // Get the total number of files in this data source
    override suspend fun getNumberOfElements(): Result<Int> {
        return try {
            val listResult = firebaseStorageRoot.listAll().await()
            Result.success(listResult.items.size)
        } catch (e: Exception) {
            logger.logError("Failed to get size of stored data", e)
            Result.failure(e)
        }
    }

    // Update a file (generic) in Firebase Storage with the given id
    override suspend fun update(id: String, data: D): Result<Unit> {
        return save(id, data)
    }
}
