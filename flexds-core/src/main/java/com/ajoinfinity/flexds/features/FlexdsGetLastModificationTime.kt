package com.ajoinfinity.flexds.features

interface FlexdsGetLastModificationTime {
    abstract suspend fun getLastModificationTime(): Result<Long>

}
