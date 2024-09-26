package com.ajoinfinity.flexds.features.size

import com.ajoinfinity.flexds.main.Flexds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SizeDecorator<D>(
    val fds: Flexds<D>,
    private val getSize: (D) -> Long,
    private val sizeDelegate: SizeDelegate<D> = SizeDelegate(fds, getSize)
): Flexds<D> by fds {
    class DataSourceSizeNotInitializedException: Exception("Datasource size not initialized")
    class DataSourceSizeUnknownException: Exception("Datasource size is unknown")

    init {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                println("getFlexdsSize: ${fds.getFlexdsSize()}")
                if (fds.getFlexdsSize().exceptionOrNull() !is DataSourceSizeNotInitializedException) {
                    val message = "SizeDecorator.kt: SizeDecorator was already applied."
                    val e = IllegalArgumentException(message)
                    logger.logError(message, e)
                    throw e
                }
            } catch (e: NotImplementedError) {
                // everything ok
            }
        }
    }

    override suspend fun getItemSize(data: D): Result<Long> {
        return sizeDelegate.getItemSize(data)
    }

    override suspend fun getItemSize(id: String): Result<Long> {
        return sizeDelegate.getItemSize(id)
    }

    override suspend fun getFlexdsSize(): Result<Long> {
        return sizeDelegate.getFlexdsSize()
    }

    override suspend fun findById(id: String): Result<D> {
        val item = try {
            fds.findById(id)
        } catch (e: Exception) {
            //logger.logError("SizeDecorator: findById: Could not find $dataTypeName $id")
            Result.failure(e)
        }
        if (item.isSuccess) try {
            sizeDelegate.insertSizeInHashMapIfNotExist(id, item.getOrThrow())
        } catch (e: Exception) {
            logger.logError("Error while inserting size into hashmap", null)
            sizeDelegate.sizeMap.remove(id)
        }
        return item
    }

    override suspend fun delete(id: String): Result<String> {
        val result = fds.delete(id)
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

    override suspend fun update(id: String, data: D): Result<D> {
        val result = fds.update(id, data)
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

    override suspend fun save(id: String, data: D): Result<D> {
        val result = fds.save(id, data)
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