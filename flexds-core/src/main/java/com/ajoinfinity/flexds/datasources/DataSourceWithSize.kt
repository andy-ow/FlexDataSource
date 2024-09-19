package com.ajoinfinity.flexds.datasources

import com.ajoinfinity.flexds.DataSource

interface DataSourceWithSize<D>: DataSource<D> {
    suspend fun getDataSourceSize(): Result<Long>
}