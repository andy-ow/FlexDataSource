package com.ajoinfinity.flexds.combinedDS

import com.ajoinfinity.flexds.datasources.DataSourceWithCache
import com.ajoinfinity.flexds.basedatasources.FilesystemDS
import com.ajoinfinity.flexds.basedatasources.MemoryDS
import kotlinx.serialization.KSerializer
import java.io.File


class FilesystemDSwithMemoryCacheFactory<D> {
    fun create(filesDir: File,
               dataSourceId: String,
               serializer: KSerializer<D>,
               getSize: (D) -> Long,
               cacheSizeInMb: Long = 10): DataSourceWithCache<D> {
        val dataType = serializer.descriptor.serialName
        val cache = MemoryDS<D>("cache", dataTypeName = dataType)
        val fsdswc = FilesystemDS<D>(
            dataSourceId = dataSourceId,
            filesDir = filesDir,
            serializer = serializer,
            dataTypeName = dataType
            )
            .addCache(cache, getSize = getSize, cacheSizeInMb = cacheSizeInMb, )
        println("Datasource.showDataflow: ${fsdswc.showDataflow()}")
        return fsdswc
    }
}