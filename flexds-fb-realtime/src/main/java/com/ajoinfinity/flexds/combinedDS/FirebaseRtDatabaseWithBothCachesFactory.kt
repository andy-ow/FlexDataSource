package com.ajoinfinity.flexds.basedatasources

import com.ajoinfinity.flexds.datasources.DataSourceWithCache
import com.google.firebase.database.FirebaseDatabase
import java.io.File

class FirebaseRtDatabaseWithBothCachesFactory<D>(
    private val database: FirebaseDatabase,
    private val clazz: Class<D>
) {
    fun create(path: File, cacheDataSourceId: String = "firebase_storage_cache", dataSourceId: String = "firebase_storage", filesystemCacheSizeInMb: Long = 50, memoryCacheSizeInMb: Long = 5): DataSourceWithCache<D> {
        val filesystemCache = FilesystemDS<D>(
            fdsId = cacheDataSourceId,
            filesDir = path
        )

        val memoryCache = MemoryDS<D>("memory_cache")

        return FirebaseRtDatabaseDS<D>(
            database = database,
            fdsId = dataSourceId,
            clazz = clazz
        )
            .addCache(filesystemCache, filesystemCacheSizeInMb) // First apply filesystem cache
            .addCache(memoryCache, memoryCacheSizeInMb) // Then apply memory cache
    }
}
