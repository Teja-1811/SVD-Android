package com.svd.svdagencies.utils

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.View
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.MaterialColors

object ActionButtonColors {
    private val outlineColors = java.util.WeakHashMap<View, Int>()
    private val imageFills = java.util.WeakHashMap<View, Int>()
    fun apply(view: View, confirmation: Boolean = false, forceDestructive: Boolean = false) {
        if (view !is android.widget.Button && view !is android.widget.ImageButton) return
        if (view is android.widget.CompoundButton || CompactUi.isDateField(view)) return
        val name = runCatching { view.resources.getResourceEntryName(view.id) }.getOrDefault("")
        if (name == "text_input_end_icon" || name == "text_input_start_icon") {
            androidx.core.view.ViewCompat.setBackgroundTintList(view, null)
            view.backgroundTintList = null
            if (imageFills.remove(view) != null || view.background !is android.graphics.drawable.ColorDrawable) {
                view.background = android.graphics.drawable.ColorDrawable(Color.TRANSPARENT)
            }
            (view as? android.widget.ImageView)?.imageTintList = ColorStateList.valueOf(
                MaterialColors.getColor(view, com.google.android.material.R.attr.colorPrimary)
            )
            return
        }
        // Toolbar icons share the header surface; do not turn navigation into filled cards.
        var parent = view.parent
        while (parent is View) {
            if (parent is androidx.appcompat.widget.Toolbar) return
            parent = parent.parent
        }
        val label = (view as? TextView)?.text?.toString().orEmpty() + " " + view.contentDescription?.toString().orEmpty()
        val words = " " + (name.replace(Regex("([a-z0-9])([A-Z])"), "$1 $2") + " " + label)
            .lowercase(java.util.Locale.ROOT).replace(Regex("[^a-z0-9]+"), " ").trim() + " "
        fun has(vararg terms: String) = terms.any { words.contains(" $it ") }
        val destructive = forceDestructive || has("delete", "remove", "reject", "revoke", "disable", "deactivate", "decrease", "decrement", "minus") ||
            label.trim() == "-" || label.trim() == "−" || has("cancel order", "cancel subscription")
        val increment = has("increase", "increment", "plus") || label.trim() == "+"
        val secondary = has("cancel", "close", "back", "clear", "reset", "logout", "log out", "sign out", "skip", "previous", "next", "qr")
        val warning = has("edit", "change", "freeze", "lock")
        val success = confirmation || increment || has("save", "submit", "confirm", "generate", "update", "approve", "collect", "pay", "payment", "settle", "complete", "accept", "restore", "unfreeze", "unlock", "enable", "activate", "whatsapp", "call")
        val info = has("download", "export", "print", "share", "email", "support", "help", "info", "report", "statement", "history")
        val skyUpdate = name == "btnGenerateBill" && label.trim().startsWith("Update", true)
        val fill = when {
            destructive || name == "btnClearSelection" -> Color.rgb(220, 53, 69)
            skyUpdate -> Color.rgb(125, 211, 252)
            secondary -> Color.rgb(108, 117, 125)
            warning -> Color.rgb(181, 129, 5)
            success -> Color.rgb(25, 135, 84)
            info -> Color.rgb(8, 121, 144)
            else -> Color.rgb(13, 110, 253)
        }
        if (view.tag?.toString()?.startsWith("svd-icon-only-outline") == true) {
            val states = arrayOf(intArrayOf(-android.R.attr.state_enabled), intArrayOf())
            val outline = ColorStateList(states, intArrayOf(ColorUtils.setAlphaComponent(fill, 90), fill))
            if (view is MaterialButton) {
                view.backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
                view.strokeWidth = view.resources.displayMetrics.density.toInt().coerceAtLeast(1)
                view.strokeColor = outline
                view.iconTint = outline
                view.setTextColor(outline)
            } else if (view is android.widget.ImageButton) {
                androidx.core.view.ViewCompat.setBackgroundTintList(view, null)
                if (outlineColors[view] != fill) {
                    val shape = android.graphics.drawable.GradientDrawable().apply {
                        cornerRadius = 8 * view.resources.displayMetrics.density
                        setColor(Color.TRANSPARENT)
                        setStroke(view.resources.displayMetrics.density.toInt().coerceAtLeast(1), outline)
                    }
                    view.background = android.graphics.drawable.RippleDrawable(
                        ColorStateList.valueOf(ColorUtils.setAlphaComponent(fill, 40)), shape, null)
                    outlineColors[view] = fill
                }
                view.imageTintList = outline
            }
            return
        }
        val foreground = if (skyUpdate && !destructive) Color.rgb(12, 74, 110) else Color.WHITE
        val states = arrayOf(intArrayOf(-android.R.attr.state_enabled), intArrayOf())
        val background = ColorStateList(states, intArrayOf(ColorUtils.setAlphaComponent(fill, 90), fill))
        val ink = ColorStateList(states, intArrayOf(ColorUtils.setAlphaComponent(foreground, 160), foreground))
        if (view is com.google.android.material.floatingactionbutton.FloatingActionButton) {
            view.backgroundTintList = background
            view.imageTintList = ink
            return
        }
        if (view is android.widget.ImageButton && imageFills[view] != fill) {
            view.background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = 10 * view.resources.displayMetrics.density
                setColor(background)
            }
            imageFills[view] = fill
        }
        androidx.core.view.ViewCompat.setBackgroundTintList(view, background)
        (view as? TextView)?.let {
            it.setTextColor(ink)
            androidx.core.widget.TextViewCompat.setCompoundDrawableTintList(it, ink)
        }
        if (view is MaterialButton) { view.backgroundTintList = background; view.iconTint = ink; view.strokeWidth = 0 }
        if (view is android.widget.ImageView) view.imageTintList = ink
    }
}

