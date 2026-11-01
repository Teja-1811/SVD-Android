package com.svd.svdagencies.utils

import android.graphics.Canvas
import android.graphics.Paint
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.CheckedTextView
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.MaterialColors
import com.google.android.material.internal.NavigationMenuItemView
import com.google.android.material.navigation.NavigationView
import java.util.WeakHashMap

/** Indent the complete destination row, including its icon, beneath its section. */
object SidebarTreeStyle {
    private val installed = WeakHashMap<NavigationView, Boolean>()
    private fun isChild(view: View): Boolean =
        (view as? NavigationMenuItemView)?.itemData?.title?.toString()?.startsWith("    ") == true

    fun install(nav: NavigationView) {
        if (installed.put(nav, true) == true) return
        fun dp(value: Int) = (value * nav.resources.displayMetrics.density + .5f).toInt()
        nav.setItemIconSize(dp(18))
        nav.itemIconPadding = dp(8)
        nav.itemVerticalPadding = dp(4)
        val list = nav.getChildAt(0) as? RecyclerView ?: return
        fun styleRows() {
            for (index in 0 until list.childCount) {
                val row = list.getChildAt(index) as? NavigationMenuItemView ?: continue
                val child = isChild(row)
                val start = dp(if (child) 40 else 12)
                if (row.paddingStart != start || row.paddingEnd != dp(12) || row.paddingTop != dp(4)) {
                    row.setPaddingRelative(start, dp(4), dp(12), dp(4))
                }
                if (row.minimumHeight != dp(40)) row.minimumHeight = dp(40)
                fun textInside(view: View) {
                    if (view is CheckedTextView) {
                        val size = if (child) 13f else 14f
                        val pixels = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, size, nav.resources.displayMetrics)
                        if (kotlin.math.abs(view.textSize - pixels) > .5f) view.setTextSize(TypedValue.COMPLEX_UNIT_SP, size)
                        val label = row.itemData?.title?.toString()?.trimStart().orEmpty()
                        if (view.text.toString() != label) view.text = label
                    } else if (view is ViewGroup) {
                        for (i in 0 until view.childCount) textInside(view.getChildAt(i))
                    }
                }
                textInside(row)
            }
        }
        list.viewTreeObserver.addOnGlobalLayoutListener { styleRows() }
        list.addOnChildAttachStateChangeListener(object : RecyclerView.OnChildAttachStateChangeListener {
            override fun onChildViewAttachedToWindow(view: View) { view.post { styleRows() } }
            override fun onChildViewDetachedFromWindow(view: View) = Unit
        })
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ColorUtils.setAlphaComponent(MaterialColors.getColor(nav, com.google.android.material.R.attr.colorOnSurface), 60)
            strokeWidth = dp(1).toFloat()
        }
        list.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun onDraw(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
                for (index in 0 until parent.childCount) {
                    val row = parent.getChildAt(index)
                    if (!isChild(row)) continue
                    val rtl = row.layoutDirection == View.LAYOUT_DIRECTION_RTL
                    val x = if (rtl) row.right - dp(28).toFloat() else row.left + dp(28).toFloat()
                    val end = if (rtl) row.right - dp(36).toFloat() else row.left + dp(36).toFloat()
                    val center = (row.top + row.bottom) / 2f
                    val continues = index + 1 < parent.childCount && isChild(parent.getChildAt(index + 1))
                    canvas.drawLine(x, row.top.toFloat(), x, if (continues) row.bottom.toFloat() else center, paint)
                    canvas.drawLine(x, center, end, center, paint)
                }
            }
        })
        list.post { styleRows() }
    }
}
