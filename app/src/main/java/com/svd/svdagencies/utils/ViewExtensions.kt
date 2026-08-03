package com.svd.svdagencies.utils

import android.content.Context
import android.graphics.Color
import android.view.View
import android.view.inputmethod.InputMethodManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicatorSpec
import com.google.android.material.progressindicator.IndeterminateDrawable
import com.svd.svdagencies.R

/**
 * Extension function to show a loading state on a MaterialButton.
 * It swaps the button text, disables it, and adds a progress spinner icon.
 */
fun MaterialButton.showLoading(isLoading: Boolean, loadingText: String? = null) {
    if (isLoading) {
        // Store original state if not already stored
        if (this.tag == null) {
            this.tag = arrayOf(this.text, this.isEnabled, this.icon)
        }
        
        this.isEnabled = false
        this.text = loadingText ?: "Processing..."
        
        val spec = CircularProgressIndicatorSpec(context, null).apply {
            indicatorColors = intArrayOf(currentTextColor)
            indicatorSize = (textSize * 1.2).toInt()
            trackThickness = (indicatorSize / 6).coerceAtLeast(1)
        }
        val progressDrawable = IndeterminateDrawable.createCircularDrawable(context, spec)
        
        this.icon = progressDrawable
        this.iconGravity = MaterialButton.ICON_GRAVITY_TEXT_START
    } else {
        // Restore original state
        (this.tag as? Array<*>)?.let {
            this.text = it[0] as? CharSequence
            this.isEnabled = it[1] as? Boolean ?: true
            this.icon = it[2] as? android.graphics.drawable.Drawable
            this.tag = null
        }
    }
}
