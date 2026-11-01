package com.svd.svdagencies.utils

import java.util.Locale

/** Shared category ordering for the item catalogue and stock entry. */
object ItemCategoryOrder {
    private val categories = listOf("milk", "curd", "buckets", "cups", "ghee", "flavoured milk", "panner", "sweets", "others")
    fun rank(category: String): Int = categories.indexOf(category.trim().lowercase(Locale.ROOT)).let {
        if (it >= 0) it else Int.MAX_VALUE
    }
}
