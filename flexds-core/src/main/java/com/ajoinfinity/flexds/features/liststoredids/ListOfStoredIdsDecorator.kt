package com.ajoinfinity.flexds.features.liststoredids

import com.ajoinfinity.flexds.Flexds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ListOfStoredIdsDecorator<D>(
    private val fds: Flexds<D>
) : BaseListOfStoredIdsDecorator<D>(fds) {

    private var storedIds: MutableList<String>? = null

    init {
        CoroutineScope(Dispatchers.IO).launch {
            initializeStoredIds()
        }
    }

    private suspend fun initializeStoredIds() {
        storedIds = try {
            fds.listStoredIds().getOrNull()?.toMutableList()
        } catch (e: Exception) {
            logger.logError("Error initializing stored ids", e)
            null
        }
    }

    private suspend fun invalidateStoredIds() {
        storedIds = null
        initializeStoredIds()
    }

    override suspend fun containsId(id: String): Result<Boolean> {
        return storedIds?.let { ids ->
            Result.success(ids.contains(id))
        } ?: run {
            invalidateStoredIds()
            fds.containsId(id)
        }
    }

    override suspend fun findById(id: String): Result<D> {
        val result = fds.findById(id)
        if (result.isSuccess) {
            val itemExists = storedIds?.contains(id) ?: false
            if (!itemExists) {
                storedIds?.add(id)
            }
        } else {
            storedIds?.remove(id)
        }
        return result
    }

    override suspend fun delete(id: String): Result<String> {
        val result = fds.delete(id)
        if (result.isSuccess) {
            storedIds?.remove(id)
        } else {
            invalidateStoredIds()
        }
        return result
    }

    override suspend fun listStoredIds(): Result<List<String>> {
        return storedIds?.let {
            Result.success(it)
        } ?: run {
            invalidateStoredIds()
            fds.listStoredIds()
        }
    }

    override suspend fun update(id: String, data: D): Result<D> {
        val result = fds.update(id, data)
        if (result.isSuccess) {
            val itemExists = storedIds?.contains(id) ?: false
            if (!itemExists) {
                storedIds?.add(id)
            }
        } else {
            invalidateStoredIds()
        }
        return result
    }

    override suspend fun save(id: String, data: D): Result<D> {
        val result = fds.save(id, data)
        if (result.isSuccess) {
            storedIds?.add(id)
        } else {
            invalidateStoredIds()
        }
        return result
    }
}