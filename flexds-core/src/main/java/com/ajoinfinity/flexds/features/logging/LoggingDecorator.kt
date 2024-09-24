package com.ajoinfinity.flexds.features.logging

import com.ajoinfinity.flexds.features.addMetadata.FdsMetadata
import com.ajoinfinity.flexds.main.Flexds

class LoggingDecorator<D>(
    private val fds: Flexds<D>,
    private val prefix: String
) : Flexds<D> {

    private fun log(funcname: String, message: String) {
        logger.log("$prefix: $funcname: $message")
    }

    // Helper function to handle logging for suspending functions
    private suspend fun <R> logAndExecute(funcname: String, block: suspend () -> R): R {
        log(funcname, "started")
        return block().also { result -> log(funcname, "result: $result") }
    }

    override val SHOULD_NOT_BE_USED_AS_CACHE: Boolean
        get() = fds.SHOULD_NOT_BE_USED_AS_CACHE

    override val fdsId: String
        get() = fds.fdsId

    override val name: String
        get() = fds.name

    override val dataTypeName: String
        get() = fds.dataTypeName

    override suspend fun containsId(id: String): Result<Boolean> {
        return logAndExecute("containsId(id: $id)") { fds.containsId(id) }
    }

    override suspend fun findById(id: String): Result<D> {
        return logAndExecute("findById(id: $id)") { fds.findById(id) }
    }

    override suspend fun save(id: String, data: D): Result<D> {
        return logAndExecute("save(id: $id, data: $data)") { fds.save(id, data) }
    }

    override suspend fun update(id: String, data: D): Result<D> {
        return logAndExecute("update(id: $id, data: $data)") { fds.update(id, data) }
    }

    override suspend fun delete(id: String): Result<String> {
        return logAndExecute("delete(id: $id)") { fds.delete(id) }
    }

    override fun clearCacheStats() {
        log("clearCacheStats", "started")
        fds.clearCacheStats()
    }

    override suspend fun deleteAll(): Result<Unit> {
        return logAndExecute("deleteAll") { fds.deleteAll() }
    }

    override suspend fun deleteMetadata(id: String): Result<String> {
        return logAndExecute("deleteMetadata(id: $id)") { fds.deleteMetadata(id) }
    }

    override suspend fun displayCacheStats() {
        log("displayCacheStats", "started")
        fds.displayCacheStats()
    }

    override suspend fun findByIdMetadata(id: String): Result<FdsMetadata> {
        return logAndExecute("findByIdMetadata(id: $id)") { fds.findByIdMetadata(id) }
    }

    override fun getDbLastModificationTimeMetadataPath(): String {
        return fds.getDbLastModificationTimeMetadataPath().also {
            log("getDbLastModificationTimeMetadataPath", "result: $it")
        }
    }

    override suspend fun getFlexdsSize(): Result<Long> {
        return logAndExecute("getFlexdsSize") { fds.getFlexdsSize() }
    }

    override suspend fun getFlexdsUsageInPercentage(): Result<Double> {
        return logAndExecute("getFlexdsUsageInPercentage") { fds.getFlexdsUsageInPercentage() }
    }

    override suspend fun getItemSize(data: D): Result<Long> {
        return logAndExecute("getItemSize(data: $data)") { fds.getItemSize(data) }
    }

    override suspend fun getItemSize(id: String): Result<Long> {
        return logAndExecute("getItemSize(id: $id)") { fds.getItemSize(id) }
    }

    override suspend fun getLastModificationTime(): Result<Long> {
        return logAndExecute("getLastModificationTime") { fds.getLastModificationTime() }
    }

    override suspend fun getMaxSize(): Result<Long> {
        return logAndExecute("getMaxSize") { fds.getMaxSize() }
    }

    override suspend fun getNumberOfElements(): Result<Int> {
        return logAndExecute("getNumberOfElements") { fds.getNumberOfElements() }
    }

    override suspend fun listStoredIds(): Result<List<String>> {
        return logAndExecute("listStoredIds") { fds.listStoredIds() }
    }

    override suspend fun saveMetadata(id: String, data: FdsMetadata): Result<FdsMetadata> {
        return logAndExecute("saveMetadata(id: $id, data: $data)") { fds.saveMetadata(id, data) }
    }

    override suspend fun setMaxSize(newMaxSize: Long): Result<Unit> {
        return logAndExecute("setMaxSize(newMaxSize: $newMaxSize)") { fds.setMaxSize(newMaxSize) }
    }

    override fun showDataflow(): String {
        return fds.showDataflow().also { log("showDataflow", "result: $it") }
    }
}
