package com.sizesapp.ui.navigation

import java.net.URLDecoder
import java.net.URLEncoder
import kotlin.text.Charsets.UTF_8

object Destinations {
    const val HOME = "home"
    const val SCAN = "scan"
    const val RECOMMEND = "recommend"
    const val SETTINGS = "settings"

    const val ITEM_EDIT_ROUTE = "itemEdit"
    const val ARG_ITEM_ID = "itemId"
    const val ARG_PHOTO_PATH = "photoPath"
    const val ARG_BRAND = "brand"
    const val ARG_SIZE_LABEL = "sizeLabel"
    const val ARG_SIZE_SYSTEM = "sizeSystem"
    const val ARG_RAW_TEXT = "rawText"

    const val ITEM_EDIT_PATTERN =
        "$ITEM_EDIT_ROUTE?$ARG_ITEM_ID={$ARG_ITEM_ID}&$ARG_PHOTO_PATH={$ARG_PHOTO_PATH}" +
            "&$ARG_BRAND={$ARG_BRAND}&$ARG_SIZE_LABEL={$ARG_SIZE_LABEL}" +
            "&$ARG_SIZE_SYSTEM={$ARG_SIZE_SYSTEM}&$ARG_RAW_TEXT={$ARG_RAW_TEXT}"

    fun newItemRoute(
        photoPath: String? = null,
        brand: String? = null,
        sizeLabel: String? = null,
        sizeSystem: String? = null,
        rawText: String? = null,
    ): String {
        val params = listOfNotNull(
            photoPath?.let { "$ARG_PHOTO_PATH=${it.encode()}" },
            brand?.let { "$ARG_BRAND=${it.encode()}" },
            sizeLabel?.let { "$ARG_SIZE_LABEL=${it.encode()}" },
            sizeSystem?.let { "$ARG_SIZE_SYSTEM=${it.encode()}" },
            rawText?.let { "$ARG_RAW_TEXT=${it.encode()}" },
        ).joinToString("&")
        return if (params.isEmpty()) ITEM_EDIT_ROUTE else "$ITEM_EDIT_ROUTE?$params"
    }

    fun editItemRoute(itemId: Long): String = "$ITEM_EDIT_ROUTE?$ARG_ITEM_ID=$itemId"

    fun String.encode(): String = URLEncoder.encode(this, UTF_8.name())
    fun String.decodeArg(): String = URLDecoder.decode(this, UTF_8.name())
}
