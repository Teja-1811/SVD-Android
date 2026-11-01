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
        var dropdownSkin = false
    }
    private val states = WeakHashMap<View, State>()
    private val datePattern = Regex("(?:\\d{4}-\\d{2}-\\d{2}|\\d{1,2}[-/]\\d{1,2}[-/]\\d{4}|\\d{1,2} [A-Za-z]{3,9} \\d{4}|[A-Za-z]{3,9} \\d{4})")

    private fun state(view: View) = states.getOrPut(view) {
        State(if (view.id == View.NO_ID) "" else runCatching { view.resources.getResourceEntryName(view.id) }.getOrDefault(""))
    }
    private fun dp(view: View, value: Int) = (value * view.resources.displayMetrics.density + .5f).toInt()
    private const val INPUT_HEIGHT_DP = 32
    private const val DROPDOWN_HEIGHT_DP = 44
    private const val DROPDOWN_RADIUS_DP = 12

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
        if (view is MaterialButton && view.tag?.toString()?.startsWith("svd-icon-only") == true) styleIconOnly(view, info)
        if (view is TextView) {
            if (view !is Button && view !is EditText &&
                info.name in CompactCopy.hiddenIds) {
                if (view.visibility != View.GONE) view.visibility = View.GONE
                return
            }
            if (view is AutoCompleteTextView || view.parent is Spinner) {
                center(view)
                if (view.parent is Spinner) {
                    if (view.background != null) view.background = null
                    view.minimumHeight = dp(view, DROPDOWN_HEIGHT_DP)
                    view.setPadding(dp(view, 4), 0, dp(view, 4), 0)
                    view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                    view.gravity = Gravity.CENTER_VERTICAL
                    view.textAlignment = View.TEXT_ALIGNMENT_VIEW_START
                }
            }
        }
        if (view is EditText && view !is AutoCompleteTextView) {
            val height = dp(view, if (view.tag == "svd-auth-large") 56 else if (info.name == "etCollectedAmount") 48 else INPUT_HEIGHT_DP)
            view.minimumHeight = height
            view.layoutParams?.let { params ->
                if (params.height != height) { params.height = height; view.layoutParams = params }
            }
            view.setPadding(view.paddingLeft, 0, view.paddingRight, 0)
            view.gravity = (view.gravity and Gravity.HORIZONTAL_GRAVITY_MASK) or Gravity.CENTER_VERTICAL
        }
        if ((view is AutoCompleteTextView || view is Spinner) ) {
            val owner = owner(view)
            val base = if (info.name.contains("year", true)) 112 else 144
            compactWidth(owner, dp(view, (base * view.resources.configuration.fontScale.coerceAtLeast(1f)).toInt()))
            val fieldHeight = dp(view, DROPDOWN_HEIGHT_DP)
            if (view.minimumHeight != fieldHeight) view.minimumHeight = fieldHeight
            view.layoutParams?.let { params ->
                if (params.height != fieldHeight) { params.height = fieldHeight; view.layoutParams = params }
            }
            if (view is Spinner && !info.dropdownSkin) {
                info.dropdownSkin = true
                val outline = GradientDrawable().apply {
                    setColor(color(view, com.google.android.material.R.attr.colorSurface))
                    setStroke(dp(view, 1), ColorUtils.setAlphaComponent(color(view, com.google.android.material.R.attr.colorOnSurface), 70))
                    cornerRadius = dp(view, DROPDOWN_RADIUS_DP).toFloat()
                }
                val arrow = AppCompatResources.getDrawable(view.context, R.drawable.ic_dropdown_arrow)!!.mutate()
                arrow.setTint(color(view, com.google.android.material.R.attr.colorPrimary))
                view.backgroundTintList = null
                view.background = android.graphics.drawable.LayerDrawable(arrayOf(outline, arrow)).apply {
                    setLayerSize(1, dp(view, 20), dp(view, 20))
                    setLayerGravity(1, Gravity.END or Gravity.CENTER_VERTICAL)
                    setLayerInsetEnd(1, dp(view, 6))
                }
                view.setPadding(dp(view, 6), 0, dp(view, 28), 0)
                view.setPopupBackgroundResource(R.drawable.bg_delivery_dropdown)
            }
            if (view is AutoCompleteTextView) {
                if (view.paddingLeft != dp(view, 6) || view.paddingTop != 0 || view.paddingRight != dp(view, 6) || view.paddingBottom != 0)
                    view.setPadding(dp(view, 6), 0, dp(view, 6), 0)
                view.gravity = Gravity.CENTER_VERTICAL
                view.textAlignment = View.TEXT_ALIGNMENT_VIEW_START
                if (!info.dropdownSkin) {
                    info.dropdownSkin = true
                    view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                    val box = owner as? TextInputLayout
                    if (box != null) {
                        val placeholder = box.hint ?: view.hint
                        box.isHintEnabled = false
                        view.hint = placeholder
                        box.minimumHeight = fieldHeight
                        box.setPadding(0, 0, 0, 0)
                        box.boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
                        box.setBoxCornerRadii(dp(view, DROPDOWN_RADIUS_DP).toFloat(), dp(view, DROPDOWN_RADIUS_DP).toFloat(), dp(view, DROPDOWN_RADIUS_DP).toFloat(), dp(view, DROPDOWN_RADIUS_DP).toFloat())
                        box.boxBackgroundColor = color(view, com.google.android.material.R.attr.colorSurface)
                        val border = ColorUtils.setAlphaComponent(color(view, com.google.android.material.R.attr.colorOnSurface), 70)
                        box.setBoxStrokeColorStateList(ColorStateList.valueOf(border))
                        box.boxStrokeWidth = dp(view, 1)
                        box.boxStrokeWidthFocused = dp(view, 1)
                        view.background = null
                        box.endIconMinSize = fieldHeight
                        box.setEndIconTintList(ColorStateList.valueOf(color(view, com.google.android.material.R.attr.colorPrimary)))
                    } else {
                        view.background = GradientDrawable().apply {
                            setColor(color(view, com.google.android.material.R.attr.colorSurface))
                            setStroke(dp(view, 1), ColorUtils.setAlphaComponent(color(view, com.google.android.material.R.attr.colorOnSurface), 70))
                            cornerRadius = dp(view, DROPDOWN_RADIUS_DP).toFloat()
                        }
                        val arrow = AppCompatResources.getDrawable(view.context, R.drawable.ic_dropdown_arrow)?.mutate()?.apply { setTint(color(view, com.google.android.material.R.attr.colorPrimary)) }
                        view.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, arrow, null)
                        view.compoundDrawablePadding = dp(view, 4)
                    }
                }
            }
        }
        if (isDateField(view) && view !is AutoCompleteTextView) {
            info.date = true
            styleDate(view as TextView, info)
        }
        if (isRefreshControl(view)) {
            info.refresh = true
            styleRefresh(view, info)
        }
        if (view is Button || view is ImageButton) {
            val headerMenu = view is ImageButton && (
                view.parent is androidx.appcompat.widget.Toolbar ||
                info.name in setOf("btnMenu", "btnMenuToggle", "btnDrawer", "btnNavigation", "menuButton")
            )
            val size = dp(view, if (headerMenu || view.tag == "svd-header-outline") 40 else if (view.tag == "svd-auth-large") 56 else if (info.name in setOf("btnClearSelection", "btnGenerateBill") && view.rootView.findViewById<View>(R.id.etCollectedAmount) != null) 48 else 32)
            view.minimumHeight = size
            view.layoutParams?.let { params ->
                params.height = size
                if (view is ImageButton || (view is MaterialButton && view.text.isNullOrBlank())) {
                    params.width = size
                    view.minimumWidth = size
                }
                view.layoutParams = params
            }
            if (headerMenu && view is ImageButton) {
                view.scaleType = if (view.tag == "svd-header-outline") ImageView.ScaleType.CENTER_INSIDE else ImageView.ScaleType.FIT_CENTER
                val padding = dp(view, if (view.tag == "svd-header-outline") 8 else 4)
                view.setPadding(padding, padding, padding, padding)
            }
            if (view is MaterialButton) { view.insetTop = 0; view.insetBottom = 0 }
            if (!headerMenu) view.setPadding(view.paddingLeft, 0, view.paddingRight, 0)
        }
        if (view.tag == "svd-create-field") {
            view.minimumHeight=dp(view,44)
            view.layoutParams?.let { params -> if(params.height!=dp(view,44)){params.height=dp(view,44);view.layoutParams=params} }
            if(view is TextView) view.setPadding(dp(view,10),0,dp(view,10),0)
        }
        if (!info.date) ActionButtonColors.apply(view)
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
        // Month dropdowns keep the same presentation as route and customer dropdowns.
        if (view is AutoCompleteTextView && "month" in name && "date" !in name) return false
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
        view.iconSize = dp(view, if (view.layoutParams?.height == dp(view, 32)) 16 else 20); view.iconPadding = 0
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
        view.minimumHeight = dp(view, INPUT_HEIGHT_DP)
        view.layoutParams?.let { it.height = dp(view, INPUT_HEIGHT_DP); view.layoutParams = it }
        if (view.layoutParams?.height?.let { it > 0 } == true && view.resources.configuration.fontScale > 1.2f)
            view.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
        if (view.transformationMethod !== DateChipPresentation) view.transformationMethod = DateChipPresentation
        if (!info.dateSkin) {
            info.dateSkin = true
            val primary = color(view, com.google.android.material.R.attr.colorPrimary)
            val surface = android.graphics.Color.WHITE
            val states = arrayOf(intArrayOf(-android.R.attr.state_enabled), intArrayOf())
            val foreground = ColorStateList(states, intArrayOf(ColorUtils.setAlphaComponent(primary, 100), primary))
            val stroke = ColorStateList(states, intArrayOf(ColorUtils.setAlphaComponent(primary, 60), ColorUtils.setAlphaComponent(primary, 145)))
            val iconSize = if (view.resources.configuration.fontScale > 1.2f) 16 else 18
            val padding = if (view.resources.configuration.fontScale > 1.2f) 2 else 8
            view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            view.setTypeface(view.typeface, android.graphics.Typeface.BOLD)
            view.setTextColor(foreground)
            if (input != null) {
                input.setBoxCornerRadii(dp(view, 8).toFloat(), dp(view, 8).toFloat(), dp(view, 8).toFloat(), dp(view, 8).toFloat())
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
                view.strokeWidth = dp(view, 1); view.strokeColor = stroke; view.cornerRadius = dp(view, 8)
                view.insetTop = 0; view.insetBottom = 0
            } else {
                if (input == null) {
                    androidx.core.view.ViewCompat.setBackgroundTintList(view, null)
                    view.backgroundTintList = null
                    val shape = GradientDrawable().apply {
                        cornerRadius = dp(view, 8).toFloat()
                        setColor(surface); setStroke(dp(view, 1), stroke)
                    }
                    view.background = RippleDrawable(ColorStateList.valueOf(color(view, androidx.appcompat.R.attr.colorControlHighlight)), shape, null)
                }
                view.setCompoundDrawablesRelative(icon, null, null, null)
                view.compoundDrawablePadding = dp(view, 4)
                androidx.core.widget.TextViewCompat.setCompoundDrawableTintList(view, foreground)
            }
            view.setPadding(0, 0, 0, 0)
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
            view.icon = icon; view.iconTint = tint; view.iconSize = dp(view, if (view.layoutParams?.height == dp(view, 32)) 16 else 20); view.iconPadding = 0
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
