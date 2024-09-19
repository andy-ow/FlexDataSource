package com.ajoinfinity.flexds

import com.ajoinfinity.flexds.basedatasources.FilesystemDS
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

        // Builder pattern for creating FlexDS with decorators
        class FlexDSBuilder<D> private constructor(private val fds: Flexds<D>) {
            private var decoratedFds: Flexds<D> = fds

            // Static method to initiate MemoryDS Builder
            companion object {
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
            }

            // Add a size decorator
            fun withSizeDecorator(getItemSize: (D) -> Long): FlexDSBuilder<D> {
                decoratedFds = SizeDecorator(decoratedFds, getItemSize)
                return this
            }

            // Add a max size decorator
            fun withMaxSizeDecorator(maxSize: Long, percentToRemove: Double = 0.5, shouldPreventSaveWhenExceeded: Boolean = false): FlexDSBuilder<D> {
                decoratedFds = MaxSizeDecorator(decoratedFds, maxSize, percentToRemove, shouldPreventSaveWhenExceeded)
                return this
            }

            // Add a cache decorator
            fun withCacheDecorator(cache: Flexds<D>): FlexDSBuilder<D> {
                decoratedFds = AddCacheDecorator(decoratedFds, cache)
                return this
            }

            // Add last modification time decorator
            fun withLastModificationTimeDecorator(dbLastModificationTimeFds: Flexds<String>): FlexDSBuilder<D> {
                decoratedFds = GetDbLastModificationTimeDecorator(dbLastModificationTimeFds, decoratedFds)
                return this
            }

            // Build the final Flexds instance
            fun build(): Flexds<D> {
                return decoratedFds
            }
        }
    }
}
