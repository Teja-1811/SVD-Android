package com.svd.svdagencies.utils

import android.view.View
import androidx.recyclerview.widget.RecyclerView

/** One vertical scrolling owner for the stock form and its summaries. */
object StockEntryScrolling {
    @JvmStatic fun install(scroll: androidx.core.widget.NestedScrollView, list: RecyclerView, rowsAdapter: RecyclerView.Adapter<*>) {
        val column = scroll.getChildAt(0) as android.view.ViewGroup
        val parent = scroll.parent as android.view.ViewGroup
        val index = parent.indexOfChild(scroll)
        val listParams = scroll.layoutParams
        val children = (0 until column.childCount).map { column.getChildAt(it) }
        children.forEach { column.removeView(it) }
        val header = children.first()
        val footer = android.widget.LinearLayout(scroll.context).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            layoutParams = RecyclerView.LayoutParams(-1, -2)
        }
        children.filter { it !== header && it !== list }.forEach { footer.addView(it) }
        fun section(view: View) = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun getItemCount() = 1
            override fun onCreateViewHolder(parent: android.view.ViewGroup, type: Int): RecyclerView.ViewHolder {
                view.layoutParams = RecyclerView.LayoutParams(-1, -2)
                return object : RecyclerView.ViewHolder(view) {}
            }
            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) = Unit
        }
        parent.removeView(scroll)
        parent.addView(list, index, listParams)
        list.isNestedScrollingEnabled = true
        list.itemAnimator = null
        list.descendantFocusability = android.view.ViewGroup.FOCUS_AFTER_DESCENDANTS
        list.adapter = androidx.recyclerview.widget.ConcatAdapter(section(header), rowsAdapter, section(footer))
    }
}
