package com.ajoinfinity.flexds.datasources

import com.ajoinfinity.flexds.Cache
import com.ajoinfinity.flexds.DataSource

interface DataSourceWithCache<D> : DataSource<D>, Cache<D> {
    suspend fun clearCache(): Result<Unit>
}
