package com.ajoinfinity.flexds.features.getdblastmodificationtime

import com.ajoinfinity.flexds.Flexds

class GetDbLastModificationTimeDecorator<D>(
    private val fds: Flexds<D>,
) : Flexds<D> by fds {
    private val modificationTimeDelegate = GetDbLastModificationTimeDelegate(fds)

    override fun getDbLastModificationTimeMetadataPath(): String {
        return modificationTimeDelegate.dbLastModificationTimeMetadataPath
    }

    override suspend fun delete(id: String): Result<String> {
        val result = fds.delete(id)
        modificationTimeDelegate.updateModificationTime()
        return result
    }

    override suspend fun update(id: String, data: D): Result<D> {
        val result = fds.update(id, data)
        modificationTimeDelegate.updateModificationTime()
        return result
    }

    override suspend fun save(id: String, data: D): Result<D> {
        val result = fds.save(id, data)
        modificationTimeDelegate.updateModificationTime()
        return result
    }

    override suspend fun getLastModificationTime(): Result<Long> {
        return modificationTimeDelegate.getLastModificationTime()
    }

}
