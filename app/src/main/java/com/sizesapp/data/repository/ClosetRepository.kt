package com.sizesapp.data.repository

import com.sizesapp.data.db.ClosetDao
import com.sizesapp.data.db.ClosetItem
import com.sizesapp.data.db.ClothingCategory
import kotlinx.coroutines.flow.Flow

class ClosetRepository(private val dao: ClosetDao) {

    fun observeAll(): Flow<List<ClosetItem>> = dao.observeAll()

    fun observeKnownBrands(): Flow<List<String>> = dao.observeKnownBrands()

    suspend fun getById(id: Long): ClosetItem? = dao.getById(id)

    suspend fun findByCategoryAndBrand(category: ClothingCategory, brand: String): List<ClosetItem> =
        dao.findByCategoryAndBrand(category, brand)

    suspend fun findByCategory(category: ClothingCategory): List<ClosetItem> = dao.findByCategory(category)

    suspend fun save(item: ClosetItem): Long = dao.upsert(item.copy(updatedAt = System.currentTimeMillis()))

    suspend fun delete(item: ClosetItem) = dao.delete(item)

    suspend fun isEmpty(): Boolean = dao.count() == 0
}
