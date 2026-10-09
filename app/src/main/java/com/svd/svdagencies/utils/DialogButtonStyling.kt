package com.svd.svdagencies.utils

import android.content.res.ColorStateList
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.svd.svdagencies.R

/** Shows a destructive confirmation with the positive action styled in red. */
fun AlertDialog.Builder.showDestructiveDialog() {
    val dialog = create()
    dialog.setOnShowListener {
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.apply {
            backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(context, R.color.brand_red)
            )
            setTextColor(ContextCompat.getColor(context, R.color.white))
        }
    }
    dialog.show()
}
