package com.svd.svdagencies.utils

import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.ColorUtils
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.MaterialColors
import com.google.android.material.textfield.TextInputLayout
import com.svd.svdagencies.R
import java.util.WeakHashMap

/** Shared presentation rules. Never changes input types, listeners, values or enabled state. */
object CompactUi {
    private class State(val name: String) {
        var font: Float? = null
        var fontApplied = false
        var date = false
        var refresh = false
        var dateSkin = false
        var refreshSkin = false
        var iconSkin = false
    }
    private val states = WeakHashMap<View, State>()
    private val datePattern = Regex("(?:\\d{4}-\\d{2}-\\d{2}|\\d{1,2}[-/]\\d{1,2}[-/]\\d{4}|\\d{1,2} [A-Za-z]{3,9} \\d{4}|[A-Za-z]{3,9} \\d{4})")

    private fun state(view: View) = states.getOrPut(view) {
        State(if (view.id == View.NO_ID) "" else runCatching { view.resources.getResourceEntryName(view.id) }.getOrDefault(""))
    }
    private fun dp(view: View, value: Int) = (value * view.resources.displayMetrics.density + .5f).toInt()
    private fun color(view: View, attribute: Int) = MaterialColors.getColor(view.context, attribute, 0)

    @JvmStatic fun applyTree(root: View) {
        apply(root)
        if (root is ViewGroup && root !is Spinner) for (index in 0 until root.childCount) applyTree(root.getChildAt(index))
    }

    internal fun applyForMeasure(root: ViewGroup) {
        apply(root)
        for (index in 0 until root.childCount) {
            val child = root.getChildAt(index)
            if (child is CompactLinearLayout) apply(child) else applyTree(child)
        }
    }

    private fun apply(view: View) {
        val info = state(view)
        if (view is MaterialButton && view.tag == "svd-icon-only") styleIconOnly(view, info)
        if (view is TextView) {
            if (view !is Button && view !is EditText &&
                info.name in CompactCopy.hiddenIds) {
                if (view.visibility != View.GONE) view.visibility = View.GONE
                return
            }
            if (view is AutoCompleteTextView || view.parent is Spinner) {
                center(view)
                if (view.parent is Spinner && view.background != null) view.background = null
            }
        }
        if ((view is AutoCompleteTextView || view is Spinner) && !isDateField(view)) {
            val owner = owner(view)
            val base = if (info.name.contains("year", true)) 112 else 144
            compactWidth(owner, dp(view, (base * view.resources.configuration.fontScale.coerceAtLeast(1f)).toInt()))
            if (view.minimumHeight != dp(view, 48)) view.minimumHeight = dp(view, 48)
            if (view is AutoCompleteTextView && (view.paddingLeft != dp(view, 8) || view.paddingRight != dp(view, 8)))
                view.setPadding(dp(view, 8), view.paddingTop, dp(view, 8), view.paddingBottom)
        }
        if (isDateField(view)) {
            info.date = true
            styleDate(view as TextView, info)
        }
        if (isRefreshControl(view)) {
            info.refresh = true
            styleRefresh(view, info)
        }
    }

    private fun center(view: TextView) {
        if (view.gravity != Gravity.CENTER) view.gravity = Gravity.CENTER
        if (view.textAlignment != View.TEXT_ALIGNMENT_CENTER) view.textAlignment = View.TEXT_ALIGNMENT_CENTER
    }

    @JvmStatic fun isDateField(view: View): Boolean {
        if (state(view).date) return true
        if (view !is TextView) return false
        if (view !is EditText && view !is Button && !view.isClickable) return false
        val name = state(view).name.lowercase()
        val dateName = name.replace("update", "").replace("validate", "")
        if ("refresh" in name || state(view).name in CompactCopy.refreshIds) return false
        if (listOf("prev", "next", "clear", "reset", "delete", "save").any { it in name }) return false
        val hint = view.hint?.toString().orEmpty().lowercase()
        val text = view.text.toString().trim()
        return "date" in dateName || "month" in name || "date" in hint || "month" in hint ||
            datePattern.matches(text) || text.startsWith("Choose month", true) || text.startsWith("Select month", true) ||
            text.startsWith("Delivery date:", true) || text.startsWith("Select date", true) || text.startsWith("Choose date", true)
    }

    @JvmStatic fun isRefreshControl(view: View): Boolean {
        if (state(view).refresh) return true
        if (view !is Button && view !is ImageView) return false
        val name = state(view).name
        val description = view.contentDescription?.toString().orEmpty()
        val text = (view as? TextView)?.text?.toString().orEmpty().trim()
        return name in CompactCopy.refreshIds || name.contains("refresh", true) ||
            description.startsWith("Refresh", true) || text.equals("Refresh", true) || text.startsWith("Reload", true)
    }

    internal fun fieldInside(view: View): View? {
        if (isDateField(view) || isRefreshControl(view) || view is AutoCompleteTextView || view is Spinner) return view
        if (view is TextInputLayout) return view.editText?.takeIf { isDateField(it) || it is AutoCompleteTextView }
        if (view is FrameLayout) {
            for (index in 0 until view.childCount) fieldInside(view.getChildAt(index))?.let { return it }
        }
        return null
    }

    private fun owner(view: View): View {
        val parent = view.parent
        if (parent is TextInputLayout) return parent
        if (parent is FrameLayout && parent.parent is TextInputLayout) return parent.parent as View
        return view
    }

    private fun managedWidth(view: View): Boolean {
        var current: View? = view
        while (current != null) {
            if (current.tag == "svd-full-width" || current.tag == "svd-fixed-row") return true
            current = current.parent as? View
        }
        return false
    }
    private fun styleIconOnly(view: MaterialButton, info: State) {
        if (view.text.isNotBlank()) { view.contentDescription = view.text; view.text = "" }
        if (info.iconSkin) return
        info.iconSkin = true
        view.iconSize = dp(view, 20); view.iconPadding = 0
        view.iconGravity = MaterialButton.ICON_GRAVITY_TEXT_START
        view.setPadding(dp(view, 8), 0, dp(view, 8), 0)
        view.gravity = Gravity.CENTER
    }
    private fun compactWidth(view: View, width: Int) {
        if (managedWidth(view)) return
        val params = view.layoutParams ?: return
        var changed = false
        if (params.width != width) { params.width = width; changed = true }
        if (params is LinearLayout.LayoutParams && params.weight != 0f) { params.weight = 0f; changed = true }
        if (changed) view.layoutParams = params
    }

    private fun skin(view: View): RippleDrawable {
        val surface = color(view, com.google.android.material.R.attr.colorSurfaceVariant)
        val shape = GradientDrawable().apply { cornerRadius = dp(view, 16).toFloat(); setColor(surface) }
        return RippleDrawable(ColorStateList.valueOf(color(view, androidx.appcompat.R.attr.colorControlHighlight)), shape, null)
    }

    private fun styleDate(view: TextView, info: State) {
        val owner = owner(view)
        val input = owner as? TextInputLayout
        val hasCalendarEndIcon = input?.endIconDrawable != null && input.endIconMode == TextInputLayout.END_ICON_CUSTOM
        val hasOtherEndIcon = input?.endIconDrawable != null && !hasCalendarEndIcon
        val shown = DateChipPresentation.display(view.text)
        val base = if (hasOtherEndIcon || shown.length > 13) 152 else 128
        compactWidth(owner, dp(view, (base * view.resources.configuration.fontScale.coerceAtLeast(1f)).toInt()))
        center(view)
        if (view.minimumHeight != dp(view, 48)) view.minimumHeight = dp(view, 48)
        if (view.layoutParams?.height?.let { it > 0 } == true && view.resources.configuration.fontScale > 1.2f)
            view.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
        if (view.transformationMethod !== DateChipPresentation) view.transformationMethod = DateChipPresentation
        if (!info.dateSkin) {
            info.dateSkin = true
            val primary = color(view, com.google.android.material.R.attr.colorPrimary)
            val surface = color(view, com.google.android.material.R.attr.colorSurface)
            val states = arrayOf(intArrayOf(-android.R.attr.state_enabled), intArrayOf())
            val foreground = ColorStateList(states, intArrayOf(ColorUtils.setAlphaComponent(primary, 100), primary))
            val stroke = ColorStateList(states, intArrayOf(ColorUtils.setAlphaComponent(primary, 60), ColorUtils.setAlphaComponent(primary, 145)))
            val iconSize = if (view.resources.configuration.fontScale > 1.2f) 16 else 18
            val padding = if (view.resources.configuration.fontScale > 1.2f) 2 else 8
            view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            view.setTypeface(view.typeface, android.graphics.Typeface.BOLD)
            view.setTextColor(foreground)
            if (input != null) {
                input.setBoxCornerRadii(dp(view, 24).toFloat(), dp(view, 24).toFloat(), dp(view, 24).toFloat(), dp(view, 24).toFloat())
                input.boxStrokeWidth = dp(view, 1); input.boxStrokeWidthFocused = dp(view, 1)
                input.setBoxStrokeColorStateList(stroke)
                input.boxBackgroundColor = surface
                // Existing custom calendar end icons have no action listeners; date inputs own the picker.
                if (hasCalendarEndIcon) input.isEndIconVisible = false
            }
            val icon = AppCompatResources.getDrawable(view.context, R.drawable.ic_calendar)?.mutate()?.apply {
                setTintList(foreground)
                setBounds(0, 0, dp(view, iconSize), dp(view, iconSize))
            }
            if (view is MaterialButton) {
                view.icon = icon; view.iconSize = dp(view, iconSize); view.iconPadding = dp(view, 4)
                view.iconGravity = MaterialButton.ICON_GRAVITY_TEXT_START
                view.iconTint = foreground
                view.backgroundTintList = ColorStateList.valueOf(surface)
                view.strokeWidth = dp(view, 1); view.strokeColor = stroke; view.cornerRadius = dp(view, 24)
                view.insetTop = dp(view, 4); view.insetBottom = dp(view, 4)
            } else {
                if (input == null) {
                    androidx.core.view.ViewCompat.setBackgroundTintList(view, null)
                    view.backgroundTintList = null
                    val shape = GradientDrawable().apply {
                        cornerRadius = dp(view, 24).toFloat()
                        setColor(surface); setStroke(dp(view, 1), stroke)
                    }
                    view.background = RippleDrawable(ColorStateList.valueOf(color(view, androidx.appcompat.R.attr.colorControlHighlight)), shape, null)
                }
                view.setCompoundDrawablesRelative(icon, null, null, null)
                view.compoundDrawablePadding = dp(view, 4)
                androidx.core.widget.TextViewCompat.setCompoundDrawableTintList(view, foreground)
            }
            view.setPadding(dp(view, padding), dp(view, 4), dp(view, padding), dp(view, 4))
        }
    }

    private fun styleRefresh(view: View, info: State) {
        compactWidth(view, dp(view, 48))
        if (view.minimumWidth != dp(view, 48)) view.minimumWidth = dp(view, 48); if (view.minimumHeight != dp(view, 48)) view.minimumHeight = dp(view, 48)
        val params = view.layoutParams
        if (params != null && params.height != dp(view, 48)) { params.height = dp(view, 48); view.layoutParams = params }
        if (view.contentDescription.isNullOrBlank()) view.contentDescription =
            (view as? TextView)?.text?.takeIf { it.isNotBlank() } ?: "Refresh"
        if (view is TextView && view.text.isNotEmpty()) view.text = ""
        if (info.refreshSkin) return
        info.refreshSkin = true
        val foreground = color(view, com.google.android.material.R.attr.colorOnSecondaryContainer)
        val background = color(view, com.google.android.material.R.attr.colorSecondaryContainer)
        val tint = ColorStateList(arrayOf(intArrayOf(-android.R.attr.state_enabled), intArrayOf()),
            intArrayOf(ColorUtils.setAlphaComponent(foreground, 90), foreground))
        val fill = ColorStateList(arrayOf(intArrayOf(-android.R.attr.state_enabled), intArrayOf()),
            intArrayOf(ColorUtils.setAlphaComponent(background, 120), background))
        val icon = AppCompatResources.getDrawable(view.context, R.drawable.ic_refresh)?.mutate()
        if (view is MaterialButton) {
            view.icon = icon; view.iconTint = tint; view.iconSize = dp(view, 20); view.iconPadding = 0
            view.iconGravity = MaterialButton.ICON_GRAVITY_TEXT_START
            view.backgroundTintList = fill; view.strokeWidth = 0; view.cornerRadius = dp(view, 12)
            view.insetTop = 0; view.insetBottom = 0; view.setPadding(dp(view, 14), 0, dp(view, 14), 0)
            view.gravity = Gravity.CENTER
        } else if (view is ImageView) {
            view.setImageDrawable(icon); view.imageTintList = tint; view.background = skin(view)
            view.setPadding(dp(view, 14), dp(view, 14), dp(view, 14), dp(view, 14))
        } else if (view is TextView) {
            icon?.setTint(foreground); icon?.setBounds(0, 0, dp(view, 20), dp(view, 20))
            view.setCompoundDrawablesRelative(icon, null, null, null); androidx.core.widget.TextViewCompat.setCompoundDrawableTintList(view, tint); view.background = skin(view)
            view.setPadding(dp(view, 14), 0, dp(view, 14), 0)
            view.gravity = Gravity.CENTER
        }
        // AppCompat's support tint can otherwise retain the original primary fill.
        if (view !is MaterialButton) {
            androidx.core.view.ViewCompat.setBackgroundTintList(view, fill)
        }
    }
}
