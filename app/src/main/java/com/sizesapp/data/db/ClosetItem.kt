package com.sizesapp.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ClothingCategory {
    SHOES, TOP, BOTTOM, DRESS, OUTERWEAR, ACCESSORY, OTHER
}

/** The size notation system as printed on the label. */
enum class SizeSystem {
    EU, US, UK, ALPHA, CM, IT, JP, OTHER
}

/** How the item actually fit the user -- the signal the recommender learns from. */
enum class FitRating {
    TOO_SMALL, SNUG_BUT_OK, TRUE_TO_SIZE, LOOSE, TOO_BIG
}

@Entity(tableName = "closet_items")
data class ClosetItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: ClothingCategory,
    val brand: String,
    val sizeLabel: String,
    val sizeSystem: SizeSystem,
    val fitRating: FitRating,
    val notes: String = "",
    val rawOcrText: String? = null,
    val photoPath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)
