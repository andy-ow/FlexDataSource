package com.ajoinfinity.flexds

import com.ajoinfinity.flexds.basedatasources.FilesystemDS
import com.ajoinfinity.flexds.basedatasources.IndexedFileFilesystemDS
import com.ajoinfinity.flexds.basedatasources.MemoryDS
import com.ajoinfinity.flexds.features.addMetadata.AddMetadataDecorator
import com.ajoinfinity.flexds.features.addcache.AddCacheDecorator
import com.ajoinfinity.flexds.features.getdblastmodificationtime.GetDbLastModificationTimeDecorator
import com.ajoinfinity.flexds.features.liststoredids.ListStoredIdsDecorator
import com.ajoinfinity.flexds.features.maxsize.MaxSizeDecorator
import com.ajoinfinity.flexds.features.size.SizeDecorator
import kotlinx.serialization.KSerializer
import java.io.File

// FlexDSBuilder class for building Flexds with decorators
class FlexDSBuilder<D>(fds: Flexds<D>) {
    private var decoratedFds: Flexds<D> = fds
    companion object {
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

    // Add a cache decorator
    fun withCache(cache: Flexds<D>): FlexDSBuilder<D> {
        decoratedFds = AddCacheDecorator(decoratedFds, cache)
        return this
    }

    // Add a cache decorator
    fun withMetadata(metadataFds: Flexds<String>): FlexDSBuilder<D> {
        decoratedFds = AddMetadataDecorator(decoratedFds, metadataFds)
        return this
    }

    // Add last modification time decorator
    fun withLastModificationTime(): FlexDSBuilder<D> {
        decoratedFds = GetDbLastModificationTimeDecorator(decoratedFds)
        return this
    }

    // Add a size decorator
    fun withListStoredIds(): FlexDSBuilder<D> {
        decoratedFds = ListStoredIdsDecorator(decoratedFds)
        return this
    }

    // Add a max size decorator
    fun withMaxSize(maxSize: Long, percentToRemove: Double = 0.5, shouldPreventSaveWhenExceeded: Boolean = false): FlexDSBuilder<D> {
        if (decoratedFds is SizeDecorator) {
            decoratedFds = MaxSizeDecorator(
                decoratedFds as SizeDecorator<D>,
                maxSize,
                percentToRemove,
                shouldPreventSaveWhenExceeded
            )
        } else {
            throw IllegalArgumentException("MaxSize feature can only be used in combination with Size feature.")
        }
        return this
    }

    // Add a size decorator
    fun withSize(getItemSize: (D) -> Long): FlexDSBuilder<D> {
        decoratedFds = SizeDecorator(decoratedFds, getItemSize)
        return this
    }

    // Build the final Flexds instance
    fun build(): Flexds<D> {
        return decoratedFds
    }
}