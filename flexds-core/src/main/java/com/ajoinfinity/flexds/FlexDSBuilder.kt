package com.ajoinfinity.flexds

import com.ajoinfinity.flexds.basedatasources.FilesystemDS
import com.ajoinfinity.flexds.basedatasources.IndexedFileFilesystemDS
import com.ajoinfinity.flexds.basedatasources.MemoryDS
import com.ajoinfinity.flexds.features.addMetadata.AddMetadataDecorator
import com.ajoinfinity.flexds.features.addcache.AddCacheDecorator
import com.ajoinfinity.flexds.features.addcache.AddUnmutableCacheDecorator
import com.ajoinfinity.flexds.features.getdblastmodificationtime.GetDbLastModificationTimeDecorator
import com.ajoinfinity.flexds.features.liststoredids.ListStoredIdsDecorator
import com.ajoinfinity.flexds.features.logging.LoggingDecorator
import com.ajoinfinity.flexds.features.maxsize.MaxSizeDecorator
import com.ajoinfinity.flexds.features.size.SizeDecorator
import com.ajoinfinity.flexds.main.Flexds
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializer
import java.io.File

// FlexDSBuilder class for building Flexds with decorators
class FlexDSBuilder<D>(
    fds: Flexds<D>,
    private val metaFds: Flexds<String>,
    private var dataClazz: Class<D>,
    private var serializer: KSerializer<D>? = null
    ) {
    private var decoratedFds: Flexds<D> = fds

    companion object {
        // Convenience methods to create builders for MemoryDS and FilesystemDS
        fun <D> memory(fdsId: String, dataClazz: Class<D>, serializer: KSerializer<D>?, ): FlexDSBuilder<D> {
            val fds = MemoryDS(fdsId, dataClazz, )
            val meta = MemoryDS("${fdsId}_metadata", String::class.java, )
            return FlexDSBuilder(fds, meta, dataClazz, serializer)
        }

        fun <D> filesystem(
            filesDir: File,
            metadataFilesdir: File,
            fdsId: String,
            //dataClass: D = "some string" as D,
            dataClazz: Class<D>,
            serializer: KSerializer<D>? = null
        ): FlexDSBuilder<D> {
            val fds = FilesystemDS(filesDir, fdsId, dataClazz, serializer)
            val meta = FilesystemDS(metadataFilesdir, fdsId, String::class.java)
            return FlexDSBuilder(fds, meta, dataClazz, serializer)
        }

        // Convenience method to create IndexedFileFilesystemDS
        fun <D> indexedFilesystem(
            filesDir: File,
            metadataFilesdir: File,
            fdsId: String,
            //dataClass: D = "some string" as D,
            dataClazz: Class<D>,
            serializer: KSerializer<D>? = null  // Serializer is required here
        ): FlexDSBuilder<D> {
            val fds = IndexedFileFilesystemDS(filesDir, fdsId, dataClazz, serializer)
            val meta = IndexedFileFilesystemDS(metadataFilesdir, fdsId, String::class.java, null)
            return FlexDSBuilder(fds, meta, dataClazz, serializer)
        }
    }

    // add logging
    fun withLogging(prefix: String = decoratedFds.name): FlexDSBuilder<D> {
        decoratedFds = LoggingDecorator<D>(decoratedFds, prefix)
        return this
    }

    // Add a cache decorator
    fun withCache(cache: Flexds<D>, unmutable: Boolean): FlexDSBuilder<D> {
        decoratedFds = if (unmutable) AddUnmutableCacheDecorator(decoratedFds, cache)
         else AddCacheDecorator(decoratedFds, cache)
        return this
    }
//--------------
    fun withCacheMemory(unmutable: Boolean): FlexDSBuilder<D> {
        return withCache(createCacheMemory(), unmutable)
    }
    fun withCacheMemoryLogged(prefix: String? = null, unmutable: Boolean): FlexDSBuilder<D> {
        return if (prefix == null) {
            withCache(LoggingDecorator<D>(createCacheMemory()), unmutable)
        } else {
            withCache(LoggingDecorator<D>(createCacheMemory(), prefix), unmutable)
        }
    }
    private fun createCacheMemory(): Flexds<D> {
        val cacheFdsId = "${decoratedFds.fdsId}-memcache"
        val cache = FlexDSBuilder
            .memory(cacheFdsId, dataClazz, serializer)
            .withMetadata()
            .build()
        return cache
//        val cache = MemoryDS<D>(cacheFdsId, dataClazz)
//        return cache
    }
//--------------
    fun withCacheInFilesystem(filesDir: File, metafilesDir: File, unmutable: Boolean): FlexDSBuilder<D> {
        return withCache(createCacheInFilesystem(filesDir, metafilesDir), unmutable)
    }
    fun withCacheInFilesystemLogged(filesDir: File, metafilesDir: File,prefix: String? = null, unmutable: Boolean): FlexDSBuilder<D> {
        return if (prefix == null) {
            withCache(LoggingDecorator<D>(createCacheInFilesystem(filesDir, metafilesDir)), unmutable)
        } else {
            withCache(LoggingDecorator<D>(createCacheInFilesystem(filesDir, metafilesDir), prefix), unmutable)
        }
    }
    private fun createCacheInFilesystem(filesDir: File, metafilesDir: File): Flexds<D> {
        val cacheFdsId = "${decoratedFds.fdsId}-filesystemcache"
        val filesystem = FlexDSBuilder
            .filesystem(filesDir, metafilesDir, cacheFdsId, dataClazz, serializer)
            .withMetadata()
            .build()
        //return FilesystemDS<D>(filesDir, cacheFdsId, dataClazz, serializer)
        return filesystem
    }
//--------------
    // Add a cache decorator
    fun withMetadata(metadataFds: Flexds<String>): FlexDSBuilder<D> {
        decoratedFds = AddMetadataDecorator(decoratedFds, metadataFds)
        return this
    }

    fun withMetadata(): FlexDSBuilder<D> {
        decoratedFds = AddMetadataDecorator(decoratedFds, metaFds)
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