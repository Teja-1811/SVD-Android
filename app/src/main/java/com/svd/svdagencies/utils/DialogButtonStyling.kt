package com.svd.svdagencies.utils

import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.svd.svdagencies.R

/** Applies the app's white, rounded surface to a confirmation or form dialog. */
fun AlertDialog.applyWhiteDialogSurface() {
    val cornerRadius = 28 * context.resources.displayMetrics.density
    window?.setBackgroundDrawable(GradientDrawable().apply {
        setColor(ContextCompat.getColor(context, R.color.white))
        this.cornerRadius = cornerRadius
    })
}

/** Shows a destructive confirmation with a white, rounded surface and red action. */
fun AlertDialog.Builder.showDestructiveDialog() {
    val dialog = create()
    dialog.setOnShowListener {
        dialog.applyWhiteDialogSurface()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.apply {
            backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(context, R.color.brand_red)
            )
            setTextColor(ContextCompat.getColor(context, R.color.white))
        }
    }
    dialog.show()
}
