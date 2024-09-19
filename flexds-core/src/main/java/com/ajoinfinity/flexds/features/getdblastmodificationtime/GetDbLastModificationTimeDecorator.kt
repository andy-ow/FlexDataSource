package com.ajoinfinity.flexds.features.getdblastmodificationtime

import com.ajoinfinity.flexds.Flexds
import com.ajoinfinity.flexds.features.FlexdsGetDbLastModificationTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GetDbLastModificationTimeDecorator<D>(
    dbLastModificationTimeFds: Flexds<String>,  // Storage for last modification time
    override val fds: Flexds<D>,
    private val modificationTimeDelegate: GetDbLastModificationTimeDelegate<D>
    = GetDbLastModificationTimeDelegate(dbLastModificationTimeFds, fds)
) : BaseGetDbLastModificationTimeDecorator<D>(fds),
    FlexdsGetDbLastModificationTime by modificationTimeDelegate {


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

}
