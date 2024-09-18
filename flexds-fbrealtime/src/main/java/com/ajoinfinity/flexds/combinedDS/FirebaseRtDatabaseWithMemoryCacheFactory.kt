package com.ajoinfinity.flexds.basedatasources

import com.ajoinfinity.flexds.DataSourceWithCache
import com.ajoinfinity.poleconyksiegowy.data.datasource.baseDS.MemoryDS
import com.google.firebase.database.FirebaseDatabase

class FirebaseRtDatabaseWithMemoryCacheFactory<D>(
    private val database: FirebaseDatabase,
    private val clazz: Class<D>
) {
    fun create(cacheSizeInMb: Long = 10): DataSourceWithCache<D> {
        val memoryCache = MemoryDS<D>("memory_cache")

        return FirebaseRtDatabaseDS(
            database = database,
            dataSourceId = "firebase_rt",
            clazz = clazz
        ).addCache(memoryCache, cacheSizeInMb, )
    }
}
