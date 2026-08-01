package com.sizesapp.ocr

import com.sizesapp.data.db.SizeSystem
import org.junit.Assert.assertEquals
import org.junit.Test

class LabelParserTest {

    @Test
    fun `Fox Ranger tag with spelled-out X-Large size`() {
        // Verbatim ML Kit output from a real Fox Ranger jersey tag scanned on
        // a Pixel 6 Pro -- "trVfetnam" / "4echo" are genuine OCR noise, kept
        // as-is so this test guards against the actual real-world input.
        val raw = """
            FOX
            RANGER
            X-Large
            oigi
            Made trVfetnam
            Fabriqué Au Vietnam
            4echo En Vietnam
        """.trimIndent()

        val parsed = LabelParser.parse(raw)

        assertEquals("Fox", parsed.guessedBrand)
        assertEquals("XL", parsed.guessedSizeLabel)
        assertEquals(SizeSystem.ALPHA, parsed.guessedSizeSystem)
    }

    @Test
    fun `spelled-out sizes normalize to abbreviations`() {
        assertEquals("XS", LabelParser.parse("Brand X-Small item").guessedSizeLabel)
        assertEquals("S", LabelParser.parse("Brand Small item").guessedSizeLabel)
        assertEquals("M", LabelParser.parse("Brand Medium item").guessedSizeLabel)
        assertEquals("L", LabelParser.parse("Brand Large item").guessedSizeLabel)
        assertEquals("XL", LabelParser.parse("Brand X-Large item").guessedSizeLabel)
        assertEquals("XXL", LabelParser.parse("Brand XX-Large item").guessedSizeLabel)
    }

    @Test
    fun `plain abbreviations still work`() {
        assertEquals("M", LabelParser.parse("adidas\nM\n100% COTTON").guessedSizeLabel)
    }

    @Test
    fun `brand name survives a dropped space from OCR`() {
        val parsed = LabelParser.parse("OFIVETEN\nUS W 7.0\nUK 4.5")
        assertEquals("Five Ten", parsed.guessedBrand)
    }
}
