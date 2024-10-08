package com.ajoinfinity.flexds.features.size

import com.ajoinfinity.flexds.main.Flexds
import com.ajoinfinity.flexds.main.FlexDataSourceManager
import com.ajoinfinity.flexds.features.size.SizeDecorator.DataSourceSizeNotInitializedException
import com.ajoinfinity.flexds.features.size.SizeDecorator.DataSourceSizeUnknownException

class SizeDelegate<D>(
    val flexds: Flexds<D>,
    val getSize: (D) -> Long,
) {
    private val logger = FlexDataSourceManager.logger
    private val NOT_INITIALIZED = -1L
    private val UNKNOWN = -2L
    internal val sizeMap = HashMap<String, Long>()
    internal val dataSourceSize = object : DataSourceSize {
        private var _size: Long = NOT_INITIALIZED
            set(value) {
                if (value == UNKNOWN) logger.logError("Datasource size was set to UNKNOWN", null)
                else if (value < 0) throw IllegalArgumentException("Datasource size was set to an illegal value: '$value'")
                field = value
            }
        override val size: Long
            get() = _size

        override fun initialize(initValue: Long) {
            if (initValue >= 0) _size = initValue
            else _size = UNKNOWN
        }

        override fun invalidate() {
            _size = UNKNOWN
        }

        override fun add(change: Long) {
            if (_size < 0) return
            _size += change
            if (_size < 0) _size = UNKNOWN
        }

        override fun subtract(change: Long) {
            add(-change)
        }
    }

     fun getItemSize(data: D): Result<Long> {
        return Result.success(getSize(data))
    }

     suspend fun getItemSize(id: String): Result<Long> {
        return getSizeFromHashMap(id)
    }

     suspend fun getFlexdsSize(): Result<Long> {
        if (dataSourceSize.size < 0) recalculateDataSourceSize()
        if (dataSourceSize.size == NOT_INITIALIZED) return Result.failure(
            DataSourceSizeNotInitializedException()
        )
        if (dataSourceSize.size == UNKNOWN) return Result.failure(DataSourceSizeUnknownException())
        return Result.success(dataSourceSize.size)
    }

    internal suspend fun getSizeFromHashMap(id: String): Result<Long> {
        return try {
            sizeMap[id]?.let {
                Result.success(it)
            } ?: run {
                val size = getSize(flexds.findById(id).getOrThrow())
                sizeMap[id] = size // Store the size in the map
                Result.success(size)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    internal fun insertSizeInHashMapIfNotExist(id: String, item: D) {
        if (id !in sizeMap) saveSizeInHashMap(id, item)
    }

    internal fun saveSizeInHashMap(id: String, item: D): Result<Long> {
        return try {
            val size = getSize(item)
            if (size >= 0) sizeMap[id] = size
            else throw IllegalArgumentException("Size of $id (item: $item) is negative: $size")
            Result.success(size)
        } catch (e: Exception) {
            logger.logError("Could not get size of $id", e)
            Result.failure(e)
        }
    }

    private suspend fun recalculateDataSourceSize() {
        try {
            val sum = flexds.listStoredIds().getOrThrow().map { getSizeFromHashMap(it) }
                .sumOf { it.getOrThrow() }
            dataSourceSize.initialize(sum)
        } catch(e: Exception) {
            dataSourceSize.invalidate()
            logger.logError("${flexds.fdsId}: Could not calculate datasource size", e)
        }
    }

    internal interface DataSourceSize {
        val size: Long

        fun initialize(initValue: Long)
        fun invalidate()
        fun add(change: Long)
        fun subtract(change: Long)
    }
}