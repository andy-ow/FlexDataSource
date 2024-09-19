package com.ajoinfinity.flexds.features

import com.ajoinfinity.flexds.FlexDataSourceManager
import com.ajoinfinity.flexds.Logger

interface FlexdsCore<D> {
    val SHOULD_NOT_BE_USED_AS_CACHE: Boolean
    val fdsId: String
    val name: String
    val dataTypeName: String // name of datatype D, which will be shown in logs or errors, maybe visible to the user, for example 'File' or 'Node'

    suspend fun containsId(id: String): Result<Boolean>
    suspend fun findById(id: String): Result<D>
    suspend fun save(id: String, data: D): Result<D>
    suspend fun update(id: String, data: D): Result<D>
    suspend fun delete(id: String): Result<String>

}