package com.ajoinfinity.flexds.basedatasources

import com.ajoinfinity.flexds.datasources.DataSourceWithCache
import com.ajoinfinity.flexds.FlexDataSourceManager
import com.google.firebase.storage.FirebaseStorage

class FirebaseStorageWithMemoryCacheFactory<D>(
    private val firebaseStorage: FirebaseStorage,
) {
    fun create(dataSourceId: String = "firebase_storage", cacheSizeInMb: Long = 10): DataSourceWithCache<D> {
        val memoryCache = MemoryDS<D>("memory_cache")

        return FirebaseStorageDS<D>(
            fdsId = dataSourceId,
            firebaseStorage = firebaseStorage,
            logger = FlexDataSourceManager.defaultLogger
        ).addCache(memoryCache, cacheSizeInMb)
    }
}
