package com.ajoinfinity.flexds.features.getdblastmodificationtime

import com.ajoinfinity.flexds.Flexds
import com.ajoinfinity.flexds.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GetDbLastModificationTimeDelegate<D>(
    private val dbLastModificationTimeFds: Flexds<String>,  // For storing the last modification time
    private val fds: Flexds<D>  // The main data source
) {
    private val logger: Logger = fds.logger
    private var lastModificationTime: Long? = null

    init {
        CoroutineScope(Dispatchers.IO).launch {
            initializeLastModificationTime()
        }
    }

    // This method retrieves the stored last modification time from dbLastModificationTimeFds
    private suspend fun initializeLastModificationTime() {
        lastModificationTime = try {
            dbLastModificationTimeFds.findById("last_modification_time").getOrNull()?.toLong()
        } catch (e: Exception) {
            logger.logError("Error initializing last modification time", e)
            null
        }
    }

    // Invalidate the cached time and reload from storage
    suspend fun invalidateLastModificationTime() {
        lastModificationTime = null
        initializeLastModificationTime()
    }

    // Get the time, first checking the cache, otherwise fetching from the dbLastModificationTimeFds
    suspend fun getTimeLastModification(): Result<Long> {
        return lastModificationTime?.let {
            Result.success(it)
        } ?: run {
            invalidateLastModificationTime()  // Invalidate the cache first
            val result = dbLastModificationTimeFds.findById("last_modification_time").mapCatching { it.toLong() }
            result.onSuccess { time ->
                lastModificationTime = time  // Update the cache after retrieving the time
            }
            result
        }
    }

    // Update the modification time to the current time and save it in dbLastModificationTimeFds
    fun updateModificationTime() {
        lastModificationTime = System.currentTimeMillis()
        saveModificationTime(lastModificationTime!!)
    }

    // Save the modification time to the storage Fds<String>
    private fun saveModificationTime(time: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                dbLastModificationTimeFds.save("last_modification_time", time.toString())
            } catch (e: Exception) {
                logger.logError("Error saving last modification time", e)
            }
        }
    }
}
