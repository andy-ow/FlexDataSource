package com.ajoinfinity.flexds.main.featureinterfaces

interface SyncCache {
    suspend fun syncCache(): Result<Unit> {
        throw NotImplementedError("Feature is not implemented. Please use SyncCacheDecorator")
    }
}