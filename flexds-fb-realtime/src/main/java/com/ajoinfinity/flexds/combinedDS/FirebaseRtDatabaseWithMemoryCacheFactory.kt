package com.ajoinfinity.flexds.basedatasources

import com.ajoinfinity.flexds.datasources.DataSourceWithCache
import com.google.firebase.database.FirebaseDatabase

class FirebaseRtDatabaseWithMemoryCacheFactory<D>(
    private val database: FirebaseDatabase,
    private val clazz: Class<D>
) {
    fun create(dataSourceId: String = "firebase_realtime", cacheSizeInMb: Long = 10): DataSourceWithCache<D> {
        val memoryCache = MemoryDS<D>("memory_cache_fb_realtime")

        return FirebaseRtDatabaseDS<D>(
            database = database,
            fdsId = dataSourceId,
            clazz = clazz
        ).addCache(memoryCache, cacheSizeInMb, )
    }
}
