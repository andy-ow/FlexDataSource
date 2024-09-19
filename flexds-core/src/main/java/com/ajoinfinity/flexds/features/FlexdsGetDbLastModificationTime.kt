package com.ajoinfinity.flexds.features

interface FlexdsGetDbLastModificationTime {
    suspend fun getLastModificationTime(): Result<Long>

}
