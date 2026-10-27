package com.svd.svdagencies.utils

import android.widget.Button
import com.google.android.material.button.MaterialButton
import com.svd.svdagencies.R

object RefreshButtonStyle {
    fun apply(button: Button) {
        button.text = ""
        button.contentDescription = "Refresh"
        val size = (48 * button.resources.displayMetrics.density).toInt()
        button.layoutParams = button.layoutParams.apply { width = size; height = size }
        button.minWidth = 0
        if (button is MaterialButton) {
            button.setIconResource(R.drawable.ic_refresh)
            button.iconPadding = 0
            button.iconTint = android.content.res.ColorStateList.valueOf(button.currentTextColor)
        } else button.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_refresh, 0, 0, 0)
    }
}
