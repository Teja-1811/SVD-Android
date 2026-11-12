package com.svd.svdagencies.utils

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TableLayout
import androidx.recyclerview.widget.RecyclerView
import java.util.WeakHashMap

/** Keep recycled table rows and their header at the same horizontal offset. */
class ScrollableTableRow @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = android.R.attr.horizontalScrollViewStyle) : HorizontalScrollView(context, attrs, defStyleAttr) {
    private class Group {
        var offset = 0
        var updating = false
        val rows = WeakHashMap<ScrollableTableRow, Boolean>()
    }
    companion object { private val groups = WeakHashMap<RecyclerView, Group>() }
    private var group: Group? = null
    init {
        isFillViewport = true
        isHorizontalScrollBarEnabled = false
        isScrollbarFadingEnabled = false
        contentDescription = "Table. Swipe sideways to see all columns."
    }
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        var owner = parent
        while (owner != null && owner !is RecyclerView) owner = owner.parent
        if (owner is RecyclerView) {
            val current = groups.getOrPut(owner) { Group() }
            group = current
            current.rows[this] = true
            post { scrollTo(current.offset, 0) }
        }
    }
    override fun onDetachedFromWindow() {
        group?.rows?.remove(this)
        group = null
        super.onDetachedFromWindow()
    }
    override fun onScrollChanged(x: Int, y: Int, oldX: Int, oldY: Int) {
        super.onScrollChanged(x, y, oldX, oldY)
        val current = group ?: return
        if (current.updating) return
        current.offset = x
        current.updating = true
        try { current.rows.keys.toList().forEach { if (it !== this) it.scrollTo(x, 0) } }
        finally { current.updating = false }
    }
}

/** Covers XML and dynamically populated Android TableLayouts. */
object ScrollableTables {
    fun install(root: View) {
        var scheduled = false
        root.viewTreeObserver.addOnGlobalLayoutListener {
            if (!scheduled) {
                scheduled = true
                root.post { scheduled = false; prepare(root) }
            }
        }
        root.post { prepare(root) }
    }
    private fun prepare(view: View) {
        if (view is TableLayout) {
            var ancestor = view.parent
            while (ancestor != null && ancestor !is HorizontalScrollView) ancestor = ancestor.parent
            if (ancestor == null) {
                val parent = view.parent as? ViewGroup ?: return
                val index = parent.indexOfChild(view)
                val params = view.layoutParams
                val scroll = HorizontalScrollView(view.context, null, android.R.attr.horizontalScrollViewStyle).apply {
                    isFillViewport = true
                    isHorizontalScrollBarEnabled = false
                    isScrollbarFadingEnabled = false
                    contentDescription = "Table. Swipe sideways to see all columns."
                }
                parent.removeViewAt(index)
                parent.addView(scroll, index, params)
                view.isShrinkAllColumns = false
                val columns = (0 until view.childCount).maxOfOrNull { (view.getChildAt(it) as? ViewGroup)?.childCount ?: 0 } ?: 0
                view.minimumWidth = (columns * 110 * view.resources.displayMetrics.density).toInt()
                scroll.addView(view, ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            }
        }
        if (view is ViewGroup) (0 until view.childCount).map { view.getChildAt(it) }.forEach { prepare(it) }
    }
}
