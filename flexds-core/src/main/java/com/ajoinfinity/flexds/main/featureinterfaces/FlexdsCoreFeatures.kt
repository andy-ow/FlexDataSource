package com.ajoinfinity.flexds.main.featureinterfaces

import com.ajoinfinity.flexds.main.FlexDataSourceManager
import com.ajoinfinity.flexds.main.logger.Logger

interface FlexdsCoreFeatures<D> {
    val logger: Logger
        get() = FlexDataSourceManager.logger
    val SHOULD_NOT_BE_USED_AS_CACHE: Boolean
    val fdsId: String
    val name: String
    val dataTypeName: String // name of datatype D, which will be shown in logs or errors, maybe visible to the user, for example 'File' or 'Node'

    suspend fun containsId(id: String): Result<Boolean>
    suspend fun findById(id: String): Result<D>
    suspend fun save(id: String, data: D): Result<D>
    suspend fun update(id: String, data: D): Result<D>
    suspend fun delete(id: String): Result<String>
    fun showDataflow(): String {
        return " --> $name "
    }

}