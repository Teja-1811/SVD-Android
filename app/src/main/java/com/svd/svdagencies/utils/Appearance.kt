package com.svd.svdagencies.utils

import android.content.Context
import com.svd.svdagencies.R

/** Appearance is device-local and survives account logout. */
object Appearance {
    private const val STORE = "svd_appearance"
    fun selected(context: Context): Int = context.getSharedPreferences(STORE, Context.MODE_PRIVATE)
        .getInt("palette", 0).coerceIn(0, 2)
    fun save(context: Context, palette: Int) {
        context.getSharedPreferences(STORE, Context.MODE_PRIVATE).edit()
            .putInt("palette", palette.coerceIn(0, 2)).apply()
    }
    fun theme(context: Context): Int = when (selected(context)) {
        1 -> R.style.Theme_SVDAgency_Ocean
        2 -> R.style.Theme_SVDAgency_Forest
        else -> R.style.Theme_SVDAgency
    }
}
