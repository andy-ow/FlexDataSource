package com.ajoinfinity.flexds

import com.ajoinfinity.flexds.features.addcache.AddCacheDecorator
import com.ajoinfinity.flexds.features.getdblastmodificationtime.GetDbLastModificationTimeDecorator
import com.ajoinfinity.flexds.features.maxsize.MaxSizeDecorator
import com.ajoinfinity.flexds.features.size.SizeDecorator

// FlexDSBuilder class for building Flexds with decorators
class FlexDSBuilder<D> internal constructor(private val fds: Flexds<D>) {
    private var decoratedFds: Flexds<D> = fds

    // Add a size decorator
    fun withSizeDecorator(getItemSize: (D) -> Long): FlexDSBuilder<D> {
        decoratedFds = SizeDecorator(decoratedFds, getItemSize)
        return this
    }

    // Add a max size decorator
    fun withMaxSizeDecorator(maxSize: Long, percentToRemove: Double = 0.5, shouldPreventSaveWhenExceeded: Boolean = false): FlexDSBuilder<D> {
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