package com.svd.svdagencies.ui.admin.sales

import java.util.Calendar

internal object CategorySalesRange {
    fun forDate(period: String, date: Calendar): Pair<Calendar, Calendar> {
        val start = date.clone() as Calendar
        val end = date.clone() as Calendar
        when (period) {
            "year" -> {
                start.set(Calendar.MONTH, Calendar.JANUARY)
                start.set(Calendar.DAY_OF_MONTH, 1)
                end.set(Calendar.MONTH, Calendar.DECEMBER)
                end.set(Calendar.DAY_OF_MONTH, 31)
            }
            "month" -> {
                start.set(Calendar.DAY_OF_MONTH, 1)
                end.set(Calendar.DAY_OF_MONTH, end.getActualMaximum(Calendar.DAY_OF_MONTH))
            }
            "week" -> {
                start.add(Calendar.DAY_OF_MONTH, -((start.get(Calendar.DAY_OF_WEEK) + 5) % 7))
                end.timeInMillis = start.timeInMillis
                end.add(Calendar.DAY_OF_MONTH, 6)
            }
        }
        for (calendar in listOf(start, end)) {
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
        }
        return start to end
    }
}
