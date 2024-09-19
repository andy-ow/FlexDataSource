package com.ajoinfinity.flexds.decorators

import com.ajoinfinity.flexds.DataSource
import com.ajoinfinity.flexds.datasources.DataSourceWithSize
import com.ajoinfinity.flexds.Logger

class DsWithSizeDecorator<D>(
    val dataSource: DataSource<D>,
    val getItemSize: (D) -> Long,
): DataSource<D>, DataSourceWithSize<D> {
    class DataSourceSizeNotInitializedException: Exception("Datasource size not initialized")
    class DataSourceSizeUnknownException: Exception("Datasource size is unknown")
    class DataSourceHasAlreadySizeFeaturesException: Exception("DataSource has already DataSourceSize features.")

    init {
        if (dataSource is DataSourceWithSize) {
            val e = DataSourceHasAlreadySizeFeaturesException()
            logger.logError("DataSourceSizeDecorator was already applied", e)
            throw e
        }
    }

    private val NOT_INITIALIZED = -1L
    private val UNKNOWN = -2L
    private val sizeMap = HashMap<String, Long>()
    private val dataSourceSize = object {
        private var _size: Long = NOT_INITIALIZED
            set(value) {
                if (value == UNKNOWN) logger.logError("Datasource size was set to UNKNOWN")
                else if (value < 0) throw IllegalArgumentException("Datasource size was set to an illegal value: '$value'")
                field = value
            }
        val size: Long
            get() = _size
        fun initialize(initValue: Long) {
            if (initValue >= 0) _size = initValue
            else _size = UNKNOWN
        }
        fun invalidate() { _size = UNKNOWN }
        fun add(change: Long) {
            if (_size < 0) return
            _size += change
            if (_size < 0) _size = UNKNOWN
        }
        fun subtract(change: Long) {
            add(-change)
        }
    }
    private suspend fun getSizeFromHashMap(id: String): Result<Long> {
        return try {
            sizeMap[id]?.let {
                Result.success(it)
            } ?: run {
                val size = getItemSize(findById(id).getOrThrow())
                sizeMap[id] = size // Store the size in the map
                Result.success(size)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun insertSizeInHashMapIfNotExist(id: String, item: D) {
            if (id !in sizeMap) saveSizeInHashMap(id, item)
    }

    private fun saveSizeInHashMap(id: String, item: D): Result<Long> {
            return try {
                val size = getItemSize(item)
                if (size >= 0) sizeMap[id] = size
                else throw IllegalArgumentException("Size of $id (item: $item) is negative: $size")
                Result.success(size)
            } catch (e: Exception) {
                logger.logError("Could not get size of $id", e)
                Result.failure(e)
            }
    }

    override suspend fun getDataSourceSize(): Result<Long> {
        if (dataSourceSize.size < 0) recalculateDataSourceSize()
        if (dataSourceSize.size == NOT_INITIALIZED) return Result.failure(DataSourceSizeNotInitializedException())
        if (dataSourceSize.size == UNKNOWN) return Result.failure(DataSourceSizeUnknownException())
        return Result.success(dataSourceSize.size)
    }
    private suspend fun recalculateDataSourceSize() {
        dataSourceSize.invalidate()
        try {
            val sum = listStoredIds().getOrThrow().map { getSizeFromHashMap(it) }
                .sumOf { it.getOrThrow() }
            dataSourceSize.initialize(sum)
        } catch(e: Exception) {
            logger.logError("$dsName: Could not calculate datasource size", e)
        }
    }
    override val SHOULD_NOT_BE_USED_AS_CACHE: Boolean =
        dataSource.SHOULD_NOT_BE_USED_AS_CACHE
    override val logger: Logger
        get() = dataSource.logger
    override val dataSourceId: String
        get() = dataSource.dataSourceId
    override val dsName: String
        get() = dataSource.dsName
    override val dataTypeName: String
        get() = dataSource.dataTypeName

    override suspend fun containsId(id: String): Result<Boolean> = dataSource.containsId(id)

    override suspend fun findById(id: String): Result<D> {
        val item = dataSource.findById(id)
        try {
            insertSizeInHashMapIfNotExist(id, item.getOrThrow())
        } catch(e: Exception) {
            // ignore
        }
        return item
    }

    override suspend fun delete(id: String): Result<Unit> {
        val result = dataSource.delete(id)
        if (result.isFailure) dataSourceSize.invalidate()
        else try {
            val change = getSizeFromHashMap(id).getOrThrow()
            dataSourceSize.subtract(change)
            sizeMap.remove(id)
        } catch(e: Exception) {
            logger.logError("Error while calculating datasource size", e)
            dataSourceSize.invalidate()
        }
        return result
    }

    override suspend fun listStoredIds(): Result<List<String>> {
        return dataSource.listStoredIds()
    }

    override suspend fun getTimeLastModification(): Result<Long> {
        return dataSource.getTimeLastModification()
    }

    override suspend fun getNumberOfElements(): Result<Int> {
        return dataSource.getNumberOfElements()
    }

    override suspend fun update(id: String, data: D): Result<Unit> {
        val result = dataSource.update(id, data)
        if (result.isFailure) dataSourceSize.invalidate()
        else try {
            val oldValue = getSizeFromHashMap(id).getOrThrow()
            dataSourceSize.subtract(oldValue)
            sizeMap.remove(id)
            val newValue = saveSizeInHashMap(id, data).getOrThrow()
            dataSourceSize.add(newValue)
        } catch(e: Exception) {
            logger.logError("Error while calculating datasource size", e)
            dataSourceSize.invalidate()
        }
        return result
    }

    override suspend fun save(id: String, data: D): Result<Unit> {
        val result = dataSource.save(id, data)
        if (result.isFailure) dataSourceSize.invalidate()
        else try {
            val newValue = saveSizeInHashMap(id, data).getOrThrow()
            dataSourceSize.add(newValue)
        } catch(e: Exception) {
            logger.logError("Error while calculating datasource size", e)
            dataSourceSize.invalidate()
        }
        return result
    }
}