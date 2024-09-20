package com.ajoinfinity.flexds

import com.ajoinfinity.flexds.basedatasources.FilesystemDS
import com.ajoinfinity.flexds.basedatasources.IndexedFileFilesystemDS
import com.ajoinfinity.flexds.basedatasources.MemoryDS
import com.ajoinfinity.flexds.features.addcache.AddCacheDecorator
import com.ajoinfinity.flexds.features.getdblastmodificationtime.GetDbLastModificationTimeDecorator
import com.ajoinfinity.flexds.features.logger.DefaultLogger
import com.ajoinfinity.flexds.features.maxsize.MaxSizeDecorator
import com.ajoinfinity.flexds.features.size.SizeDecorator
import kotlinx.serialization.KSerializer
import java.io.File


class FlexDataSourceManager {
    companion object {
        private var _logger: Logger = DefaultLogger()

        var logger: Logger
            get() = _logger
            set(value) {
                _logger = value
            }

        // Convenience methods to create builders for MemoryDS and FilesystemDS
        fun <D> memory(fdsId: String, dataTypeName: String = "Item"): FlexDSBuilder<D> {
            return FlexDSBuilder(MemoryDS(fdsId, dataTypeName))
        }

        fun <D> filesystem(
            filesDir: File,
            fdsId: String,
            dataClass: D = "some string" as D,
            serializer: KSerializer<D>? = null
        ): FlexDSBuilder<D> {
            return FlexDSBuilder(FilesystemDS(filesDir, fdsId, dataClass, serializer))
        }

        // Convenience method to create IndexedFileFilesystemDS
        fun <D> indexedFilesystem(
            filesDir: File,
            fdsId: String,
            dataClass: D = "some string" as D,
            serializer: KSerializer<D>  // Serializer is required here
        ): FlexDSBuilder<D> {
            return FlexDSBuilder(IndexedFileFilesystemDS(filesDir, fdsId, dataClass, serializer))
        }
    }
}
