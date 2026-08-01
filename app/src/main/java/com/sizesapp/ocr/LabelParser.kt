package com.sizesapp.ocr

import com.sizesapp.data.db.SizeSystem

data class ParsedLabel(
    val rawText: String,
    val guessedBrand: String? = null,
    val guessedSizeLabel: String? = null,
    val guessedSizeSystem: SizeSystem? = null,
)

/**
 * Best-effort heuristics for pulling a brand and size out of raw OCR text from
 * a clothing/shoe label. Labels are wildly inconsistent in layout, so this is
 * intentionally a *starting point* -- the scan screen always lets the user
 * review and correct the guess before saving, it never saves blind.
 */
object LabelParser {

    private val euPattern = Regex("""\bEUR?\.?\s*(\d{2,3}(?:[.,]\d)?)\b""", RegexOption.IGNORE_CASE)
    private val usPattern = Regex("""\bUS\.?\s*(\d{1,2}(?:[.,]\d)?)\b""", RegexOption.IGNORE_CASE)
    private val ukPattern = Regex("""\bUK\.?\s*(\d{1,2}(?:[.,]\d)?)\b""", RegexOption.IGNORE_CASE)
    private val cmPattern = Regex("""\b(\d{2}(?:[.,]\d)?)\s*CM\b""", RegexOption.IGNORE_CASE)
    // Longest/most-specific alternatives first, since regex alternation takes
    // the first branch that matches rather than the longest one -- otherwise
    // "XX-Large" could get short-circuited by the plain "Large" branch.
    private val alphaPattern = Regex(
        """\b(XX-?Small|X-?Small|Small|Medium|XX-?Large|X-?Large|Large|XXS|XS|S|M|L|XXXL|XXL|XL|2XL|3XL)\b""",
        RegexOption.IGNORE_CASE,
    )

    private fun normalizeAlphaSize(matched: String): String = when (matched.replace("-", "").uppercase()) {
        "XXSMALL" -> "XXS"
        "XSMALL" -> "XS"
        "SMALL" -> "S"
        "MEDIUM" -> "M"
        "LARGE" -> "L"
        "XLARGE" -> "XL"
        "XXLARGE" -> "XXL"
        else -> matched.uppercase()
    }

    fun parse(rawText: String): ParsedLabel {
        // OCR sometimes drops spaces (a logo glyph glued to the next word, a
        // line-wrap losing a space, etc.), so also try each brand name with
        // spaces stripped from both sides -- "OFIVETEN" should still match
        // "Five Ten".
        val spacelessText = rawText.replace(" ", "")
        val brand = KnownBrands.sortedByLengthDescending
            .firstOrNull { name ->
                rawText.contains(name, ignoreCase = true) ||
                    spacelessText.contains(name.replace(" ", ""), ignoreCase = true)
            }

        euPattern.find(rawText)?.let {
            return ParsedLabel(rawText, brand, it.groupValues[1].replace(",", "."), SizeSystem.EU)
        }
        usPattern.find(rawText)?.let {
            return ParsedLabel(rawText, brand, it.groupValues[1].replace(",", "."), SizeSystem.US)
        }
        ukPattern.find(rawText)?.let {
            return ParsedLabel(rawText, brand, it.groupValues[1].replace(",", "."), SizeSystem.UK)
        }
        cmPattern.find(rawText)?.let {
            return ParsedLabel(rawText, brand, it.groupValues[1].replace(",", "."), SizeSystem.CM)
        }
        alphaPattern.find(rawText)?.let {
            return ParsedLabel(rawText, brand, normalizeAlphaSize(it.groupValues[1]), SizeSystem.ALPHA)
        }

        return ParsedLabel(rawText = rawText, guessedBrand = brand)
    }
}
