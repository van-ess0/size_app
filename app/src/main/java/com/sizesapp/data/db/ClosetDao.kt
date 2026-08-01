package com.sizesapp.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ClosetDao {

    @Query("SELECT * FROM closet_items ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<ClosetItem>>

    @Query("SELECT * FROM closet_items WHERE id = :id")
    suspend fun getById(id: Long): ClosetItem?

    @Query(
        "SELECT * FROM closet_items WHERE category = :category " +
            "AND brand LIKE '%' || :brand || '%' COLLATE NOCASE ORDER BY updatedAt DESC"
    )
    suspend fun findByCategoryAndBrand(category: ClothingCategory, brand: String): List<ClosetItem>

    @Query("SELECT * FROM closet_items WHERE category = :category ORDER BY updatedAt DESC")
    suspend fun findByCategory(category: ClothingCategory): List<ClosetItem>

    @Query("SELECT DISTINCT brand FROM closet_items ORDER BY brand ASC")
    fun observeKnownBrands(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: ClosetItem): Long

    @Update
    suspend fun update(item: ClosetItem)

    @Delete
    suspend fun delete(item: ClosetItem)

    @Query("SELECT COUNT(*) FROM closet_items")
    suspend fun count(): Int
}
