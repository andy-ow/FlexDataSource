// FirebaseRtDatabaseDS.kt
package com.ajoinfinity.flexds.basedatasources

import com.ajoinfinity.flexds.main.Flexds
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import kotlinx.coroutines.tasks.await

import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.getValue
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.serialization.KSerializer

class FirebaseRtDatabaseDS<D>(
    database: FirebaseDatabase,
    override val fdsId: String,
    override val dataClazz: Class<D>,
    override val SHOULD_NOT_BE_USED_AS_CACHE: Boolean = true,
    override val unmutable: Boolean,
) : Flexds<D> {

    private val root = database.reference.child(fdsId)
    private val lastChangedRef = root.child("last_changed")
    private val nodesRef = root.child(dataClazz.simpleName)


    override fun observeDbLastModificationTime(): Flow<Long> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val time = snapshot.getValue(Long::class.java)
                if (time != null) {
                    trySend(time).isSuccess // Send the time to the flow
                }
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException()) // Close the flow with the error
            }
        }
        lastChangedRef.addValueEventListener(listener)
        awaitClose { // Suspends until the flow is closed
            lastChangedRef.removeEventListener(listener) // Clean up the listener
        }
    }

    override suspend fun getNumberOfElements(): Result<Int> {
        return try {
            val snapshot = nodesRef.get().await()
            Result.success(snapshot.childrenCount.toInt())
        } catch (e: Exception) {
            logger.logError("Failed to get number of elements", e)
            Result.failure(e)
        }
    }

    override suspend fun containsId(id: String): Result<Boolean> {
        return try {
            val exists = nodesRef.child(id).get().await().exists()
            Result.success(exists)
        } catch (e: Exception) {
            logger.logError("Failed to check if ID exists: $id", e)
            Result.failure(e)
        }
    }

    override suspend fun save(id: String, data: D): Result<D> {
        return try {
            nodesRef.child(id).setValue(data).await()
            updateLastChanged()
            Result.success(data)
        } catch (e: Exception) {
            logger.logError("Failed to save data for ID: $id", e)
            Result.failure(e)
        }
    }

    override suspend fun update(id: String, data: D): Result<D> {
        return save(id, data)  // Same logic as save

    }

    override suspend fun delete(id: String): Result<String> {
        return try {
            nodesRef.child(id).removeValue().await()
            updateLastChanged()
            Result.success(id)
        } catch (e: Exception) {
            logger.logError("Failed to delete data for ID: $id", e)
            Result.failure(e)
        }
    }

    override suspend fun findById(id: String): Result<D> {
        return try {
            val snapshot = nodesRef.child(id).get().await()
            if (snapshot.exists()) {
                val data: D? = snapshot.getValue(dataClazz)
                if (data != null) {
                    Result.success(data)
                } else {
                    val errorMsg = "Data is null for ID: $id"
                    logger.logError(errorMsg, null)
                    Result.failure(Exception(errorMsg))
                }
            } else {
                val errorMsg = "Data not found for ID: $id"
                logger.logError(errorMsg, null)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            logger.logError("Failed to find data for ID: $id", e)
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

    override suspend fun getLastModificationTime(): Result<Long> {
        return try {
            val snapshot = lastChangedRef.get().await()
            val lastModifiedTime = snapshot.getValue(Long::class.java)
            if (lastModifiedTime != null) {
                Result.success(lastModifiedTime)
            } else {
                val errorMsg = "No last modification time found"
                logger.logError(errorMsg, null)
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

