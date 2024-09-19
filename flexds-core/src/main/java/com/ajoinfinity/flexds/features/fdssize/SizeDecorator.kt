package com.ajoinfinity.flexds.features.fdssize

import com.ajoinfinity.flexds.Flexds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SizeDecorator<D>(
    private val flexDataSource: Flexds<D>,
    private val getItemSize: (D) -> Long,
    private val sizeDelegate: SizeDelegate<D> = SizeDelegate(flexDataSource, getItemSize)
): BaseSizeDecorator<D>(flexDataSource) {
    class DataSourceSizeNotInitializedException: Exception("Datasource size not initialized")
    class DataSourceSizeUnknownException: Exception("Datasource size is unknown")

    init {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                sizeDelegate.getFlexDsSize()
                val message = "SizeDecorator.kt: SizeDecorator was already applied."
                val e = IllegalArgumentException(message)
                logger.logError(message, e)
                throw e
            } catch (e: NotImplementedError) {
                // everything ok
            }
        }
    }

    override suspend fun findById(id: String): Result<D> {
        val item = flexDataSource.findById(id)
        if (item.isSuccess) try {
            sizeDelegate.insertSizeInHashMapIfNotExist(id, item.getOrThrow())
        } catch (e: Exception) {
            logger.logError("Error while inserting size into hashmap")
            sizeDelegate.sizeMap.remove(id)
        }
        return item
    }

    override suspend fun delete(id: String): Result<Unit> {
        val result = flexDataSource.delete(id)
        if (result.isFailure) sizeDelegate.dataSourceSize.invalidate()
        else try {
            val change = sizeDelegate.getSizeFromHashMap(id).getOrThrow()
            sizeDelegate.dataSourceSize.subtract(change)
            sizeDelegate.sizeMap.remove(id)
        } catch(e: Exception) {
            logger.logError("Error while calculating datasource size", e)
            sizeDelegate.dataSourceSize.invalidate()
        }
        return result
    }

    override suspend fun update(id: String, data: D): Result<Unit> {
        val result = flexDataSource.update(id, data)
        if (result.isFailure) sizeDelegate.dataSourceSize.invalidate()
        else try {
            val oldValue = sizeDelegate.getSizeFromHashMap(id).getOrThrow()
            sizeDelegate.dataSourceSize.subtract(oldValue)
            sizeDelegate.sizeMap.remove(id)
            val newValue = sizeDelegate.saveSizeInHashMap(id, data).getOrThrow()
            sizeDelegate.dataSourceSize.add(newValue)
        } catch(e: Exception) {
            logger.logError("Error while calculating datasource size", e)
            sizeDelegate.dataSourceSize.invalidate()
        }
        return result
    }

    override suspend fun save(id: String, data: D): Result<Unit> {
        val result = flexDataSource.save(id, data)
        if (result.isFailure) sizeDelegate.dataSourceSize.invalidate()
        else try {
            val newValue = sizeDelegate.saveSizeInHashMap(id, data).getOrThrow()
            sizeDelegate.dataSourceSize.add(newValue)
        } catch(e: Exception) {
            logger.logError("Error while calculating datasource size", e)
            sizeDelegate.dataSourceSize.invalidate()
        }
        return result
    }
}