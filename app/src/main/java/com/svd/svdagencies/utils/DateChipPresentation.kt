package com.svd.svdagencies.utils

import android.graphics.Rect
import android.text.method.TransformationMethod
import android.view.View
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/** Display-only formatting: getText(), text watchers and API values retain their original dates. */
object DateChipPresentation : TransformationMethod {
    private val tokens = Regex("""(?<!\d)(?:\d{4}-\d{2}-\d{2}|\d{1,2}[-/]\d{1,2}[-/]\d{4}|\d{1,2}[- ][\p{L}.]{3,12}[- ]\d{4})(?!\d)""")
    private val formats = listOf("yyyy-MM-dd", "dd-MM-yyyy", "dd/MM/yyyy", "dd MMM yyyy", "dd MMMM yyyy", "dd-MMM-yyyy", "dd-MMMM-yyyy")

    @JvmStatic fun display(value: CharSequence): String {
        val raw = value.toString()
        return tokens.replace(raw) { match ->
            var formatted: String? = null
            for (locale in listOf(Locale.getDefault(), Locale.US).distinct()) {
                for (pattern in formats) {
                    val parser = SimpleDateFormat(pattern, locale).apply {
                        isLenient = false
                        timeZone = TimeZone.getTimeZone("UTC")
                    }
                    val position = ParsePosition(0)
                    val date = parser.parse(match.value, position)
                    if (date != null && position.index == match.value.length) {
                        formatted = SimpleDateFormat("dd-MM-yyyy", Locale.US).apply {
                            timeZone = TimeZone.getTimeZone("UTC")
                        }.format(date)
                        break
                    }
                }
                if (formatted != null) break
            }
            formatted ?: match.value
        }
    }

    override fun getTransformation(source: CharSequence?, view: View?): CharSequence {
        if (source == null) return ""
        val shown = display(source)
        return if (shown == source.toString()) source else shown
    }

    override fun onFocusChanged(view: View?, sourceText: CharSequence?, focused: Boolean, direction: Int, previouslyFocusedRect: Rect?) = Unit
}
