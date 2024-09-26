package com.ajoinfinity.flexds.main.featureinterfaces

import com.ajoinfinity.flexds.main.Flexds

interface SyncCache<D> {

    suspend fun syncCachesIfNeeded(): Result<Unit> {
        throw NotImplementedError("Feature is not implemented. Please use SyncCacheDecorator")
    }

    suspend fun getLastCachesSyncTime(): Result<Set<Pair<Flexds<D>, Result<Long>>>> {
        throw NotImplementedError("Feature is not implemented. Please use SyncCacheDecorator")
    }

    val lastCacheSyncTimeMetaName: String
        get() {
        throw NotImplementedError("Feature is not implemented. Please use SyncCacheDecorator")
    }

    suspend fun syncCache(listOfCaches: List<Flexds<D>>): Result<Unit> {
        throw NotImplementedError("Feature is not implemented. Please use SyncCacheDecorator")
    }
}