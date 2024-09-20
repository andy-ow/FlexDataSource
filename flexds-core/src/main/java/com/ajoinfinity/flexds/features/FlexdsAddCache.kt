package com.ajoinfinity.flexds.features

interface FlexdsAddCache {
    suspend fun displayCacheStats() {
            throw NotImplementedError("Feature is not implemented. Please use AddCacheDecorator")
    }

    fun clearCacheStats() {
        throw NotImplementedError("Feature is not implemented. Please use AddCacheDecorator")
    }
}