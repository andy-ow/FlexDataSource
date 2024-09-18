package com.ajoinfinity.flexds.basedatasources

import com.ajoinfinity.flexds.DataSourceWithCache
import com.ajoinfinity.flexds.FlexDataSource
import com.google.firebase.storage.FirebaseStorage
import java.io.File

class FirebaseStorageWithFilesystemCacheFactory<D>(
    private val firebaseStorage: FirebaseStorage,
) {
    fun create(cachePath: File,
               dataSourceId: String = "firebase_storage",
               cacheDataSourceId: String = "firebase_storage_cache",
               cacheSizeInMb: Long = 10): DataSourceWithCache<D> {
        val filesystemCache = FilesystemDS<D>(
            dataSourceId = cacheDataSourceId,
            filesDir = cachePath
        )

        return FirebaseStorageDS<D>(
            dataSourceId = dataSourceId,
            firebaseStorage = firebaseStorage,
            logger = FlexDataSource.defaultLogger
        ).addCache(filesystemCache, cacheSizeInMb)
    }
}
