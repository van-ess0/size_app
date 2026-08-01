package com.sizesapp.data.sizing

import com.sizesapp.data.db.ClosetItem
import com.sizesapp.data.db.ClothingCategory
import com.sizesapp.data.db.FitRating
import com.sizesapp.data.db.SizeSystem
import com.sizesapp.data.repository.ClosetRepository

sealed interface Recommendation {
    /** The user already owns an item from this exact brand+category. */
    data class DirectMatch(val basedOn: ClosetItem, val note: String?) : Recommendation

    /** No item from this brand, but converted from another well-fitting item of the same category. */
    data class Converted(
        val suggestedSize: String,
        val sizeSystem: SizeSystem,
        val basedOn: ClosetItem,
        val note: String?,
    ) : Recommendation

    /** Not enough data in the closet to say anything useful yet. */
    data class NoData(val reason: String) : Recommendation
}

class SizeRecommender(private val repository: ClosetRepository) {

    suspend fun recommend(category: ClothingCategory, brand: String): Recommendation {
        val brandNote = BrandFitNotes.forBrand(brand)

        val directMatches = repository.findByCategoryAndBrand(category, brand)
        val bestDirect = directMatches.bestFitOrNull()
        if (bestDirect != null) {
            return Recommendation.DirectMatch(bestDirect, brandNote)
        }

        val allInCategory = repository.findByCategory(category)
        val reference = allInCategory.bestFitOrNull()
            ?: return Recommendation.NoData(
                "No ${category.name.lowercase()} items logged yet with a known fit. " +
                    "Scan and rate one item as 'true to size' first so future suggestions have something to compare against.",
            )

        return if (category == ClothingCategory.SHOES) {
            convertShoeSize(reference, brandNote)
        } else {
            Recommendation.Converted(
                suggestedSize = reference.sizeLabel,
                sizeSystem = reference.sizeSystem,
                basedOn = reference,
                note = buildString {
                    append("No ${brand} ${category.name.lowercase()} logged yet -- suggesting the size that fit well in ")
                    append("${reference.brand} as a starting point. Clothing sizing varies a lot between brands.")
                    if (brandNote != null) append(" $brandNote")
                },
            )
        }
    }

    private fun convertShoeSize(reference: ClosetItem, brandNote: String?): Recommendation {
        val row = ShoeSizeChart.nearestRow(reference.sizeSystem, reference.sizeLabel.toSizeValue())
            ?: return Recommendation.Converted(
                suggestedSize = reference.sizeLabel,
                sizeSystem = reference.sizeSystem,
                basedOn = reference,
                note = "Couldn't convert ${reference.sizeSystem} sizing automatically; showing the reference size as-is.",
            )
        val euValue = row.eu
        return Recommendation.Converted(
            suggestedSize = formatSize(euValue),
            sizeSystem = SizeSystem.EU,
            basedOn = reference,
            note = buildString {
                append("Converted from your ${reference.brand} size (${reference.sizeLabel} ${reference.sizeSystem}), which you rated as fitting well.")
                if (brandNote != null) append(" $brandNote")
            },
        )
    }

    private fun List<ClosetItem>.bestFitOrNull(): ClosetItem? =
        firstOrNull { it.fitRating == FitRating.TRUE_TO_SIZE }
            ?: firstOrNull { it.fitRating == FitRating.SNUG_BUT_OK }
            ?: firstOrNull { it.fitRating == FitRating.LOOSE }
}

private fun String.toSizeValue(): Double = this.replace(",", ".").filter { it.isDigit() || it == '.' }.toDoubleOrNull() ?: 0.0

private fun formatSize(value: Double): String =
    if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()
