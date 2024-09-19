package com.ajoinfinity.flexds.basedatasources

import com.ajoinfinity.flexds.datasources.DataSourceWithCache
import com.ajoinfinity.flexds.FlexDataSourceManager
import com.google.firebase.storage.FirebaseStorage
import java.io.File

class FirebaseStorageWithBothCachesFactory<D>(
    private val firebaseStorage: FirebaseStorage,
) {
    fun create(path: File,
               cacheDataSourceId: String = "firebase_storage_cache",
               dataSourceId: String = "firebase_storage",
               filesystemCacheSizeInMb: Long = 50,
               memoryCacheSizeInMb: Long = 5): DataSourceWithCache<D> {
        val filesystemCache = FilesystemDS<D>(
            fdsId = cacheDataSourceId,
            filesDir = path
        )

        val memoryCache = MemoryDS<D>("memory_cache")

        return FirebaseStorageDS<D>(
            fdsId = dataSourceId,
            firebaseStorage = firebaseStorage,
            logger = FlexDataSourceManager.defaultLogger
        )
            .addCache(filesystemCache, filesystemCacheSizeInMb)  // First filesystem cache
            .addCache(memoryCache, memoryCacheSizeInMb)      // Then memory cache
    }
}
