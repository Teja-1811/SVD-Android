package com.svd.svdagencies.utils

import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import java.util.WeakHashMap
import android.widget.TextView

/** Route legacy non-button loading messages into the same branded overlay. */
internal object LoadingStatus {
    private val loadingPattern = Regex("^Loading(?:\\s|[.\u2026]|$)", RegexOption.IGNORE_CASE)
    private val names = WeakHashMap<TextView, String>()
    private val bound = WeakHashMap<TextView, Boolean>()
    private val installed = WeakHashMap<View, Boolean>()

    /** Also catches loading labels inserted or changed after initial screen setup. */
    fun install(root: View) {
        if (installed.put(root, true) != null) return
        fun scan(view: View) {
            if (view is TextView && view !is Button && view !is EditText && !bound.containsKey(view)) {
                val name = names.getOrPut(view) { if (view.id > 0) runCatching { view.resources.getResourceEntryName(view.id) }.getOrDefault("") else "" }
                if (isLoading(view.text) || listOf("status", "message", "loading").any { name.contains(it, true) }) bind(view)
            }
            if (view is ViewGroup) for (index in 0 until view.childCount) scan(view.getChildAt(index))
        }
        var pending = false
        fun schedule() {
            if (pending) return
            pending = true
            root.post { pending = false; scan(root) }
        }
        root.viewTreeObserver.addOnGlobalLayoutListener { schedule() }
        schedule()
    }

    fun isLoading(text: CharSequence) = loadingPattern.containsMatchIn(text.toString().trim())
    fun bind(view: TextView) {
        if (bound.put(view, true) != null) return
        var active = false
        val originalAlpha = view.alpha
        fun update() {
            val loading = isLoading(view.text)
            val wanted = loading && view.isAttachedToWindow && view.isShown
            val alpha = if (loading) 0f else originalAlpha
            if (view.alpha != alpha) view.alpha = alpha
            if (wanted == active) return
            active = wanted
            if (wanted) LoadingOverlayManager.show(view) else LoadingOverlayManager.hide(view)
        }
        view.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = update()
            override fun afterTextChanged(s: Editable?) = Unit
        })
        view.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) = update()
            override fun onViewDetachedFromWindow(v: View) { active = false; LoadingOverlayManager.hide(view) }
        })
        view.viewTreeObserver.addOnGlobalLayoutListener { update() }
        update()
    }
}
