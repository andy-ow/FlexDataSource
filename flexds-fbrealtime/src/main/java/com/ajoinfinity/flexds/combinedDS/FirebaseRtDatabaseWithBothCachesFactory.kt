package com.ajoinfinity.flexds.basedatasources

import com.ajoinfinity.flexds.DataSourceWithCache
import com.ajoinfinity.poleconyksiegowy.data.datasource.baseDS.MemoryDS
import com.google.firebase.database.FirebaseDatabase
import java.io.File

class FirebaseRtDatabaseWithBothCachesFactory<D>(
    private val database: FirebaseDatabase,
    private val clazz: Class<D>
) {
    fun create(path: File, subdir: String, cacheSizeInMb: Long = 10): DataSourceWithCache<D> {
        val filesystemCache = FilesystemDS<D>(
            dataSourceId = "filesystem_cache",
            filesDir = File(path, subdir)
        )

        val memoryCache = MemoryDS<D>("memory_cache")

        return FirebaseRtDatabaseDS(
            database = database,
            dataSourceId = "firebase_rt",
            clazz = clazz
        )
            .addCache(filesystemCache, cacheSizeInMb) // First apply filesystem cache
            .addCache(memoryCache, cacheSizeInMb) // Then apply memory cache
    }
}
