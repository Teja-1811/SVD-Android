package com.svd.svdagencies.utils

import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** Date labels compatible with every Android version supported by the app. */
object PortalDates {
    fun isoDay(offsetDays: Int = 0): String {
        val day = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, offsetDays) }
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(day.time)
    }

    fun display(value: String, locale: Locale = Locale.getDefault()): String {
        if (!Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}").matches(value)) return value
        val input = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
        val position = ParsePosition(0)
        val date = input.parse(value, position) ?: return value
        if (position.index != value.length) return value
        return SimpleDateFormat("dd MMM yyyy", locale).format(date)
    }
}
