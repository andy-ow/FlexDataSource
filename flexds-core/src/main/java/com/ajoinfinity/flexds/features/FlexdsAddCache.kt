package com.ajoinfinity.flexds.features

import com.ajoinfinity.flexds.Flexds

interface FlexdsAddCache<D> {

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