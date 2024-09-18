package com.ajoinfinity.flexds.basedatasources

import com.ajoinfinity.flexds.DataSourceWithCache
import com.ajoinfinity.flexds.FlexDataSource
import com.ajoinfinity.poleconyksiegowy.data.datasource.baseDS.MemoryDS
import com.google.firebase.storage.FirebaseStorage
import java.io.File

class FirebaseStorageWithBothCachesFactory<D>(
    private val firebaseStorage: FirebaseStorage,
) {
    fun create(path: File, cacheDataSourceId: String = "firebase_storage_cache", dataSourceId: String = "firebase_storage", filesystemCacheSizeInMb: Long = 50, memoryCacheSizeInMb: Long = 5): DataSourceWithCache<D> {
        val filesystemCache = FilesystemDS<D>(
            dataSourceId = cacheDataSourceId,
            filesDir = path
        )

        val memoryCache = MemoryDS<D>("memory_cache")

        return FirebaseStorageDS<D>(
            dataSourceId = dataSourceId,
            firebaseStorage = firebaseStorage,
            logger = FlexDataSource.defaultLogger
        ).addCache(filesystemCache, filesystemCacheSizeInMb)  // First filesystem cache
            .addCache(memoryCache, memoryCacheSizeInMb)      // Then memory cache
    }
}
