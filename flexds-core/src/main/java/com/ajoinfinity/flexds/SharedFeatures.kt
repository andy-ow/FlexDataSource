package com.ajoinfinity.flexds


interface SharedFeatures<D> {
    val SHOULD_NOT_BE_USED_AS_CACHE: Boolean
    abstract val dataSourceId: String
    abstract val dsName: String
    abstract val dataTypeName: String // name of datatype D, which will be shown in logs or errors, maybe visible to the user, for example 'File' or 'Node'
    abstract val logger: Logger
    abstract suspend fun containsId(id: String): Result<Boolean>
    abstract suspend fun findById(id: String): Result<D>
    abstract suspend fun save(id: String, data: D): Result<Unit>
    abstract suspend fun update(id: String, data: D): Result<Unit>
    abstract suspend fun delete(id: String): Result<Unit>
    abstract suspend fun deleteAll(): Result<Unit>
    abstract suspend fun listStoredIds(): Result<List<String>>
    abstract suspend fun getTimeLastModification(): Result<Long>
    abstract suspend fun getNumberOfElements(): Result<Int>
    fun showDataflow(): String {
        return " --> $dsName"
    }
}

