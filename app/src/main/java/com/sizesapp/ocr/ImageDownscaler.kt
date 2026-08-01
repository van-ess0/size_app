package com.sizesapp.ocr

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

/**
 * Shrinks an image file in place if its longest side exceeds [maxDimension].
 * Camera captures can be many megapixels -- far more than ML Kit needs to
 * read printed label text -- and this is also the exact file that ends up
 * permanently stored as the closet item's photo, so doing this once here
 * keeps both OCR memory use and long-term on-device storage in check.
 */
object ImageDownscaler {

    fun downscaleInPlace(file: File, maxDimension: Int = 1600, quality: Int = 85) {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        val longestSide = max(bounds.outWidth, bounds.outHeight)
        if (longestSide <= maxDimension || longestSide <= 0) return

        val options = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(longestSide, maxDimension)
        }
        val sampled = BitmapFactory.decodeFile(file.absolutePath, options) ?: return

        val scale = maxDimension.toFloat() / max(sampled.width, sampled.height)
        val finalBitmap = if (scale < 1f) {
            Bitmap.createScaledBitmap(sampled, (sampled.width * scale).toInt(), (sampled.height * scale).toInt(), true)
        } else {
            sampled
        }

        FileOutputStream(file).use { out -> finalBitmap.compress(Bitmap.CompressFormat.JPEG, quality, out) }

        if (finalBitmap !== sampled) sampled.recycle()
        finalBitmap.recycle()
    }

    /** Power-of-two downsampling factor -- cheap, and lets the decoder itself skip pixels instead of decoding full-size. */
    private fun calculateInSampleSize(longestSide: Int, target: Int): Int {
        var sampleSize = 1
        var current = longestSide
        while (current / 2 >= target) {
            current /= 2
            sampleSize *= 2
        }
        return sampleSize
    }
}
