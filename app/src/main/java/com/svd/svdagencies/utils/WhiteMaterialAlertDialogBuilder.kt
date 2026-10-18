package com.svd.svdagencies.utils

import android.content.Context
import android.graphics.drawable.GradientDrawable
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.svd.svdagencies.R

/** Keeps every alert dialog surface white, including dialogs created before showing. */
class WhiteMaterialAlertDialogBuilder(context: Context) : MaterialAlertDialogBuilder(context) {
    init {
        setBackground(GradientDrawable().apply {
            setColor(ContextCompat.getColor(context, R.color.white))
            cornerRadius = 28 * context.resources.displayMetrics.density
        })
    }
}
