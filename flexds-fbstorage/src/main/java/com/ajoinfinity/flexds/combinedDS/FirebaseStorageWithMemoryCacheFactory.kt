package com.ajoinfinity.flexds.basedatasources

import com.ajoinfinity.flexds.DataSourceWithCache
import com.ajoinfinity.flexds.FlexDataSource
import com.ajoinfinity.poleconyksiegowy.data.datasource.baseDS.MemoryDS
import com.google.firebase.storage.FirebaseStorage

class FirebaseStorageWithMemoryCacheFactory<D>(
    private val firebaseStorage: FirebaseStorage,
) {
    fun create(dataSourceId: String = "firebase_storage", cacheSizeInMb: Long = 10): DataSourceWithCache<D> {
        val memoryCache = MemoryDS<D>("memory_cache")

        return FirebaseStorageDS<D>(
            dataSourceId = dataSourceId,
            firebaseStorage = firebaseStorage,
            logger = FlexDataSource.defaultLogger
        ).addCache(memoryCache, cacheSizeInMb)
    }
}
