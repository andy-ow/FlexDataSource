// FirebaseRtDatabaseDS.kt
package com.ajoinfinity.flexds.basedatasources

import com.ajoinfinity.flexds.DataSource
import com.ajoinfinity.flexds.FlexDataSource
import com.ajoinfinity.flexds.Logger
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

class FirebaseRtDatabaseDS<D> constructor(
    database: FirebaseDatabase,
    override val dataSourceId: String,
    private val clazz: Class<D>,
    override val dsName: String = "FirebaseRealtimeDb-'$dataSourceId'",
    override val dataTypeName: String = "Data",
    override val SHOULD_NOT_BE_USED_AS_CACHE: Boolean = true,
    override val logger: Logger = FlexDataSource.defaultLogger,
) : DataSource<D> {

    private val root = database.reference.child(dataSourceId)
    private val lastChangedRef = root.child("last_changed")
    private val nodesRef = root.child(dataTypeName)

    override suspend fun getSize(): Result<Int> {
        return try {
            val snapshot = nodesRef.get().await()
            Result.success(snapshot.childrenCount.toInt())
        } catch (e: Exception) {
            logger.logError("Failed to get size", e)
            Result.failure(e)
        }
    }

    override suspend fun containsId(id: String): Result<Boolean> {
        return try {
            val exists = nodesRef.child(id).get().await().exists()
            Result.success(exists)
        } catch (e: Exception) {
            logger.logError("Failed to check containsId for $id", e)
            Result.failure(e)
        }
    }

    override suspend fun save(id: String, data: D): Result<Unit> {
        return try {
            nodesRef.child(id).setValue(data).await()
            updateLastChanged()
            Result.success(Unit)
        } catch (e: Exception) {
            logger.logError("Failed to save data for $id", e)
            Result.failure(e)
        }
    }

    override suspend fun update(id: String, data: D): Result<Unit> {
        return save(id, data)
    }

    override suspend fun delete(id: String): Result<Unit> {
        return try {
            nodesRef.child(id).removeValue().await()
            updateLastChanged()
            Result.success(Unit)
        } catch (e: Exception) {
            logger.logError("Failed to delete data for $id", e)
            Result.failure(e)
        }
    }

    override suspend fun listStoredIds(): Result<List<String>> {
        return try {
            val snapshot = nodesRef.get().await()
            val ids = snapshot.children.mapNotNull { it.key }
            Result.success(ids)
        } catch (e: Exception) {
            logger.logError("Failed to list stored IDs", e)
            Result.failure(e)
        }
    }

    override suspend fun findById(id: String): Result<D> {
        return try {
            val snapshot = nodesRef.child(id).get().await()
            if (snapshot.exists()) {
                val data = snapshot.getValue(clazz)
                if (data != null) {
                    Result.success(data)
                } else {
                    val errorMsg = "Data is null for id $id"
                    logger.logError(errorMsg)
                    Result.failure(Exception(errorMsg))
                }
            } else {
                val errorMsg = "Data not found for id $id"
                logger.logError(errorMsg)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            logger.logError("Failed to find data for $id", e)
            Result.failure(e)
        }
    }

    override suspend fun getTimeLastModification(): Result<Long> {
        return try {
            val snapshot = lastChangedRef.get().await()
            val lastModifiedTime = snapshot.getValue(Long::class.java)
            if (lastModifiedTime != null) {
                Result.success(lastModifiedTime)
            } else {
                val errorMsg = "No last modification time found"
                logger.logError(errorMsg)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            logger.logError("Failed to get last modification time", e)
            Result.failure(e)
        }
    }

    private suspend fun updateLastChanged() {
        try {
            lastChangedRef.setValue(System.currentTimeMillis()).await()
        } catch (e: Exception) {
            logger.logError("Failed to update last changed timestamp", e)
        }
    }
}
