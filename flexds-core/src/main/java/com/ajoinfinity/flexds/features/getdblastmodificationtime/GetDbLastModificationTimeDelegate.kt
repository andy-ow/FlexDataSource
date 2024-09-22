package com.ajoinfinity.flexds.features.getdblastmodificationtime

import com.ajoinfinity.flexds.main.Flexds
import com.ajoinfinity.flexds.main.logger.Logger
import com.ajoinfinity.flexds.features.addMetadata.FdsMetadata
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GetDbLastModificationTimeDelegate<D>(
    val fds: Flexds<D>,
) {
    private val logger: Logger = fds.logger
    internal val dbLastModificationTimeMetadataPath = "last_modification_time_${fds.fdsId}"

    // Get the time, first checking the cache, otherwise fetching from the dbLastModificationTimeFds
    suspend fun getLastModificationTime(): Result<Long> {
        return fds.findByIdMetadata(dbLastModificationTimeMetadataPath)
            .mapCatching {
                it.toString().toLong() }
    }

    // Update the modification time to the current time and save it in dbLastModificationTimeFds
    suspend fun updateModificationTime() {
        saveModificationTime(System.currentTimeMillis())
    }

    // Save the modification time to the storage Fds<String>
    private suspend fun saveModificationTime(time: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                fds.saveMetadata(dbLastModificationTimeMetadataPath, FdsMetadata(time.toString()))
            } catch (e: Exception) {
                logger.logError("Error saving last modification time", e)
            }
        }
    }
}
