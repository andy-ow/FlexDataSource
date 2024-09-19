package com.ajoinfinity.flexds.features.getdblastmodificationtime

import com.ajoinfinity.flexds.Flexds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GetDbLastModificationTimeDecorator<D>(
    dbLastModificationTimeFds: Flexds<String>,  // Storage for last modification time
    override val fds: Flexds<D>
) : BaseGetDbLastModificationTimeDecorator<D>(fds) {

    private val modificationTimeDelegate = GetDbLastModificationTimeDelegate(dbLastModificationTimeFds, fds)

    override suspend fun delete(id: String): Result<String> {
        val result = fds.delete(id)
        if (result.isSuccess) {
            modificationTimeDelegate.updateModificationTime()
        } else {
            modificationTimeDelegate.invalidateLastModificationTime()
        }
        return result
    }

    override suspend fun update(id: String, data: D): Result<D> {
        val result = fds.update(id, data)
        if (result.isSuccess) {
            modificationTimeDelegate.updateModificationTime()
        } else {
            modificationTimeDelegate.invalidateLastModificationTime()
        }
        return result
    }

    override suspend fun save(id: String, data: D): Result<D> {
        val result = fds.save(id, data)
        if (result.isSuccess) {
            modificationTimeDelegate.updateModificationTime()
        } else {
            modificationTimeDelegate.invalidateLastModificationTime()
        }
        return result
    }

    override suspend fun getLastModificationTime(): Result<Long> {
        return modificationTimeDelegate.getTimeLastModification()
    }
}
