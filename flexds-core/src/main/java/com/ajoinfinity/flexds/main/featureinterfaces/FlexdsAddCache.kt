package com.ajoinfinity.flexds.main.featureinterfaces

import com.ajoinfinity.flexds.main.Flexds

interface FlexdsAddCache<D> {
    val setOfCaches: Set<Flexds<D>>
        get() {
            return emptySet()
        }

    val cache: Flexds<D>
        get() {
            throw NotImplementedError("Feature is not implemented. Please use AddCacheDecorator")
        }

    suspend fun displayCacheStats() {
        throw NotImplementedError("Feature is not implemented. Please use AddCacheDecorator")
    }

    fun clearCacheStats() {
        throw NotImplementedError("Feature is not implemented. Please use AddCacheDecorator")
    }
}