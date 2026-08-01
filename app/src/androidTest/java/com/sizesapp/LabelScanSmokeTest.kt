package com.sizesapp

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sizesapp.data.db.SizeSystem
import com.sizesapp.ocr.LabelParser
import com.sizesapp.ocr.OcrTextRecognizer
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Runs the real on-device ML Kit recognizer + [LabelParser] against photos of
 * actual clothing/shoe labels (see app/src/androidTest/assets/labels), so a
 * regression in either OCR wiring or the parsing heuristics gets caught here
 * rather than only being discovered by a human scanning a real garment.
 */
@RunWith(AndroidJUnit4::class)
class LabelScanSmokeTest {

    private fun assetToTempFile(name: String): File {
        // Assets live in this test APK, not the app-under-test -- use the
        // instrumentation's own context to read them, but the app's cache
        // dir (a real filesystem path) to stage the decoded file for OcrTextRecognizer.
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val file = File(instrumentation.targetContext.cacheDir, name)
        instrumentation.context.assets.open("labels/$name").use { input ->
            file.outputStream().use { output -> input.copyTo(output) }
        }
        return file
    }

    @Test
    fun adidasShirtTag_readsBrandAndAlphaSize() = runBlocking {
        val text = OcrTextRecognizer.recognize(assetToTempFile("adidas_shirt.jpg"))
        val parsed = LabelParser.parse(text)

        assertEquals("Adidas", parsed.guessedBrand)
        assertEquals("M", parsed.guessedSizeLabel)
        assertEquals(SizeSystem.ALPHA, parsed.guessedSizeSystem)
    }

    @Test
    fun fiveTenShoeLabel_readsBrandAndUkSizeFromMultiColumnTable() = runBlocking {
        val text = OcrTextRecognizer.recognize(assetToTempFile("fiveten_shoe.jpg"))
        val parsed = LabelParser.parse(text)

        assertEquals("Five Ten", parsed.guessedBrand)
        assertEquals("4.5", parsed.guessedSizeLabel)
        assertEquals(SizeSystem.UK, parsed.guessedSizeSystem)
    }
}
