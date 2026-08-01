package com.sizesapp.data.sizing

import com.sizesapp.data.db.SizeSystem

/**
 * One row = the same physical shoe size expressed in every system.
 * Values are the standard published conversion table (unisex adult sizing);
 * real labels sometimes differ by a half size depending on brand last, which
 * is why cross-brand suggestions are always flagged as approximate.
 */
data class ShoeSizeRow(
    val eu: Double,
    val usMens: Double,
    val usWomens: Double,
    val uk: Double,
    val footLengthCm: Double,
)

object ShoeSizeChart {

    val rows = listOf(
        ShoeSizeRow(eu = 36.0, usMens = 4.0, usWomens = 5.5, uk = 3.5, footLengthCm = 22.5),
        ShoeSizeRow(eu = 37.0, usMens = 4.5, usWomens = 6.0, uk = 4.0, footLengthCm = 23.0),
        ShoeSizeRow(eu = 37.5, usMens = 5.0, usWomens = 6.5, uk = 4.5, footLengthCm = 23.5),
        ShoeSizeRow(eu = 38.0, usMens = 5.5, usWomens = 7.0, uk = 5.0, footLengthCm = 24.0),
        ShoeSizeRow(eu = 38.5, usMens = 6.0, usWomens = 7.5, uk = 5.5, footLengthCm = 24.5),
        ShoeSizeRow(eu = 39.0, usMens = 6.5, usWomens = 8.0, uk = 6.0, footLengthCm = 25.0),
        ShoeSizeRow(eu = 40.0, usMens = 7.0, usWomens = 8.5, uk = 6.5, footLengthCm = 25.5),
        ShoeSizeRow(eu = 40.5, usMens = 7.5, usWomens = 9.0, uk = 7.0, footLengthCm = 26.0),
        ShoeSizeRow(eu = 41.0, usMens = 8.0, usWomens = 9.5, uk = 7.5, footLengthCm = 26.5),
        ShoeSizeRow(eu = 42.0, usMens = 8.5, usWomens = 10.0, uk = 8.0, footLengthCm = 27.0),
        ShoeSizeRow(eu = 42.5, usMens = 9.0, usWomens = 10.5, uk = 8.5, footLengthCm = 27.5),
        ShoeSizeRow(eu = 43.0, usMens = 9.5, usWomens = 11.0, uk = 9.0, footLengthCm = 28.0),
        ShoeSizeRow(eu = 44.0, usMens = 10.0, usWomens = 11.5, uk = 9.5, footLengthCm = 28.5),
        ShoeSizeRow(eu = 44.5, usMens = 10.5, usWomens = 12.0, uk = 10.0, footLengthCm = 29.0),
        ShoeSizeRow(eu = 45.0, usMens = 11.0, usWomens = 12.5, uk = 10.5, footLengthCm = 29.5),
        ShoeSizeRow(eu = 46.0, usMens = 12.0, usWomens = 13.5, uk = 11.5, footLengthCm = 30.5),
        ShoeSizeRow(eu = 47.0, usMens = 13.0, usWomens = 14.5, uk = 12.5, footLengthCm = 31.5),
    )

    /** Finds the row closest to [value] in the given [system], e.g. system=EU, value=42.0. */
    fun nearestRow(system: SizeSystem, value: Double): ShoeSizeRow? {
        val selector: (ShoeSizeRow) -> Double = when (system) {
            SizeSystem.EU -> ShoeSizeRow::eu
            SizeSystem.US -> ShoeSizeRow::usMens
            SizeSystem.UK -> ShoeSizeRow::uk
            SizeSystem.CM -> ShoeSizeRow::footLengthCm
            else -> return null
        }
        return rows.minByOrNull { kotlin.math.abs(selector(it) - value) }
    }

    fun valueFor(row: ShoeSizeRow, system: SizeSystem): Double? = when (system) {
        SizeSystem.EU -> row.eu
        SizeSystem.US -> row.usMens
        SizeSystem.UK -> row.uk
        SizeSystem.CM -> row.footLengthCm
        else -> null
    }
}

/**
 * Informal, non-authoritative notes on how a brand's sizing tends to run
 * relative to the standard chart. Shown as a caveat, never used for hard math.
 */
object BrandFitNotes {
    private val notes = mapOf(
        "adidas" to "Adidas shoes often run slightly small/narrow -- many wearers go up half a size.",
        "nike" to "Nike shoes are close to standard EU sizing for most models.",
        "zara" to "Zara clothing tends to run small compared to the size on the label.",
        "h&m" to "H&M clothing is usually close to true to size.",
        "levi's" to "Levi's jeans are true to size but sizing varies a lot by fit/cut (e.g. 501 vs 511).",
        "levis" to "Levi's jeans are true to size but sizing varies a lot by fit/cut (e.g. 501 vs 511).",
        "uniqlo" to "Uniqlo tends to run small/slim compared to Western brands.",
    )

    fun forBrand(brand: String): String? = notes[brand.trim().lowercase()]
}
