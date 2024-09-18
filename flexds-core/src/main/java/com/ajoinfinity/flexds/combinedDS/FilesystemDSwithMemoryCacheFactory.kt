package com.ajoinfinity.flexds.combinedDS

import com.ajoinfinity.flexds.DataSource
import com.ajoinfinity.flexds.DataSourceWithCache
import com.ajoinfinity.flexds.basedatasources.FilesystemDS
import com.ajoinfinity.poleconyksiegowy.data.datasource.baseDS.MemoryDS
import java.io.File
import java.nio.file.Files


class FilesystemDSwithMemoryCacheFactory<D> {
    fun create(path: File, subdir: String, cacheSizeInMb: Int = 10): DataSourceWithCache<D> {
        val cache = MemoryDS<D>("cache")
        return FilesystemDS<D>(
            dataSourceId = "data",
            filesDir = File("/tmp"),
            )
            .addCache(cache, 10)
    }
}