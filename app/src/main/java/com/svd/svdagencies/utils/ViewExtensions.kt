package com.svd.svdagencies.utils

import android.content.Context
import android.graphics.Color
import android.view.View
import android.view.inputmethod.InputMethodManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicatorSpec
import com.google.android.material.progressindicator.IndeterminateDrawable
import com.svd.svdagencies.R

private data class LoadingButtonState(
    val text: CharSequence,
    val enabled: Boolean,
    val icon: android.graphics.drawable.Drawable?
)

/**
 * Extension function to show a loading state on a MaterialButton.
 * It swaps the button text, disables it, and adds a progress spinner icon.
 */
fun MaterialButton.showLoading(isLoading: Boolean, loadingText: String? = null) {
    if (isLoading) {
        // Keep this state separate from View.tag, which layouts and other UI
        // helpers may already use for unrelated data.
        if (getTag(R.id.tag_loading_button_state) == null) {
            setTag(
                R.id.tag_loading_button_state,
                LoadingButtonState(this.text, this.isEnabled, this.icon)
            )
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
        progressDrawable.start()
        this.iconGravity = MaterialButton.ICON_GRAVITY_TEXT_START
    } else {
        (this.icon as? android.graphics.drawable.Animatable)?.stop()
        // Restore original state
        (getTag(R.id.tag_loading_button_state) as? LoadingButtonState)?.let { state ->
            this.text = state.text
            this.isEnabled = state.enabled
            this.icon = state.icon
            setTag(R.id.tag_loading_button_state, null)
        }
    }
}
