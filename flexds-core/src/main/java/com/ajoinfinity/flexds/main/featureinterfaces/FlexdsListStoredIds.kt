package com.ajoinfinity.flexds.main.featureinterfaces

interface FlexdsListStoredIds<D>: FlexdsCoreFeatures<D> {

    suspend fun listStoredIds(): Result<List<String>> {
            throw NotImplementedError("Feature is not implemented. Please use ListStoredIdsDecorator")
        }

    suspend fun getNumberOfElements(): Result<Int> {
        throw NotImplementedError("Feature is not implemented. Please use ListStoredIdsDecorator")
    }

    suspend fun deleteAll(): Result<Unit> {
        throw NotImplementedError("Feature is not implemented. Please use ListStoredIdsDecorator")
    }
}
