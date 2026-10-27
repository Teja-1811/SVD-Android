package com.svd.svdagencies.utils

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.AutoCompleteTextView
import android.widget.LinearLayout
import android.widget.Spinner

/** Ordinary linear layout, with wrapping only for horizontal filter rows. */
class CompactLinearLayout @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : LinearLayout(context, attrs) {
    private data class Row(val children: List<View>, val width: Int, val height: Int)
    private val rows = mutableListOf<Row>()
    private var wrapping = false
    private val gap get() = (8 * resources.displayMetrics.density + .5f).toInt()

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        CompactUi.applyForMeasure(this)
        val visible = (0 until childCount).map { getChildAt(it) }.filter { it.visibility != GONE }
        if (tag == "svd-fixed-row" && MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.UNSPECIFIED) {
            visible.filter { it.tag == "svd-refresh-share" }.forEach { child ->
                val available = (MeasureSpec.getSize(widthMeasureSpec) - paddingLeft - paddingRight).coerceAtLeast(0)
                child.layoutParams.width = maxOf((48 * resources.displayMetrics.density + .5f).toInt(), available / 10)
            }
        }
        val controls = visible.mapNotNull { CompactUi.fieldInside(it) }
        wrapping = tag != "svd-fixed-row" && orientation == HORIZONTAL && visible.size > 1 && controls.isNotEmpty()
        if (!wrapping || MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.UNSPECIFIED) {
            rows.clear(); super.onMeasure(widthMeasureSpec, heightMeasureSpec); return
        }
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val available = (width - paddingLeft - paddingRight).coerceAtLeast(0)
        rows.clear()
        var children = mutableListOf<View>(); var used = 0; var rowHeight = 0
        fun finish() {
            if (children.isNotEmpty()) rows.add(Row(children, used, rowHeight))
            children = mutableListOf(); used = 0; rowHeight = 0
        }
        visible.forEach { child ->
            val params = child.layoutParams as? MarginLayoutParams
            // This row owns its gaps; old weighted-row margins otherwise double them.
            if (params != null) { params.leftMargin = 0; params.rightMargin = 0; params.marginStart = 0; params.marginEnd = 0 }
            val horizontal = (params?.leftMargin ?: 0) + (params?.rightMargin ?: 0)
            val vertical = (params?.topMargin ?: 0) + (params?.bottomMargin ?: 0)
            val requested = child.layoutParams.width
            val limit = (available - horizontal).coerceAtLeast(0)
            val spec = if (requested > 0) MeasureSpec.makeMeasureSpec(minOf(requested, limit), MeasureSpec.EXACTLY)
                else MeasureSpec.makeMeasureSpec(limit, MeasureSpec.AT_MOST)
            val requestedHeight = child.layoutParams.height
            val heightSpec = if (requestedHeight > 0) MeasureSpec.makeMeasureSpec(requestedHeight, MeasureSpec.EXACTLY)
                else MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
            child.measure(spec, heightSpec)
            val needed = child.measuredWidth + horizontal
            if (children.isNotEmpty() && used + gap + needed > available) finish()
            if (children.isNotEmpty()) used += gap
            children.add(child); used += needed; rowHeight = maxOf(rowHeight, child.measuredHeight + vertical)
        }
        finish()
        val height = paddingTop + paddingBottom + rows.sumOf { it.height } + gap * (rows.size - 1).coerceAtLeast(0)
        setMeasuredDimension(resolveSize(width, widthMeasureSpec), resolveSize(height, heightMeasureSpec))
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        if (!wrapping) { super.onLayout(changed, left, top, right, bottom); return }
        var y = paddingTop
        val rtl = layoutDirection == LAYOUT_DIRECTION_RTL
        rows.forEach { row ->
            val space = width - paddingLeft - paddingRight - row.width
            val shift = when (Gravity.getAbsoluteGravity(gravity, layoutDirection) and Gravity.HORIZONTAL_GRAVITY_MASK) {
                Gravity.RIGHT -> space
                Gravity.CENTER_HORIZONTAL -> space / 2
                else -> 0
            }.coerceAtLeast(0)
            var x = if (rtl) width - paddingRight - shift else paddingLeft + shift
            row.children.forEach { child ->
                val params = child.layoutParams as? ViewGroup.MarginLayoutParams
                val start = if (rtl) params?.rightMargin ?: 0 else params?.leftMargin ?: 0
                val end = if (rtl) params?.leftMargin ?: 0 else params?.rightMargin ?: 0
                val verticalSpace = row.height - child.measuredHeight - (params?.topMargin ?: 0) - (params?.bottomMargin ?: 0)
                val verticalShift = when (gravity and Gravity.VERTICAL_GRAVITY_MASK) {
                    Gravity.CENTER_VERTICAL -> verticalSpace / 2
                    Gravity.BOTTOM -> verticalSpace
                    else -> 0
                }.coerceAtLeast(0)
                val childY = y + (params?.topMargin ?: 0) + verticalShift
                if (rtl) {
                    x -= start; child.layout(x - child.measuredWidth, childY, x, childY + child.measuredHeight)
                    x -= child.measuredWidth + end + gap
                } else {
                    x += start; child.layout(x, childY, x + child.measuredWidth, childY + child.measuredHeight)
                    x += child.measuredWidth + end + gap
                }
            }
            y += row.height + gap
        }
    }
}
