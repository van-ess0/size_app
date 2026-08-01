package com.sizesapp.ocr

/**
 * Brands [LabelParser] looks for by name/substring match in scanned label
 * text. Kept separate from the parsing logic so growing this list (which
 * will happen often as more real labels get scanned) doesn't require
 * touching the regex/matching code at all.
 */
object KnownBrands {

    val all = listOf(
        "Adidas", "Nike", "Puma", "Reebok", "New Balance", "Under Armour",
        "Zara", "H&M", "Uniqlo", "Levi's", "Levis", "Gap", "Mango", "Primark",
        "Champion", "Converse", "Vans", "Asics", "Columbia", "The North Face",
        "Calvin Klein", "Tommy Hilfiger", "Lacoste", "Ralph Lauren", "Five Ten", "Fox",
    )

    // Sorted once here (not on every parse() call) so multi-word brands are
    // checked before a shorter brand name that happens to be a substring of
    // another, e.g. "The North Face" before "North" would ever be added.
    val sortedByLengthDescending = all.sortedByDescending { it.length }
}
