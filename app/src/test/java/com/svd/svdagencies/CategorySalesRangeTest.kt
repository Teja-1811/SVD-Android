package com.svd.svdagencies

import com.svd.svdagencies.ui.admin.sales.CategorySalesRange
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class CategorySalesRangeTest {
    private fun check(period: String, date: String, start: String, end: String) {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val calendar = Calendar.getInstance().apply { time = format.parse(date)!! }
        val range = CategorySalesRange.forDate(period, calendar)
        assertEquals(start, format.format(range.first.time))
        assertEquals(end, format.format(range.second.time))
        assertEquals(date, format.format(calendar.time))
    }
    @Test fun yearIncludesWholeSelectedYear() = check("year", "2026-10-25", "2026-01-01", "2026-12-31")
    @Test fun monthIncludesLeapDay() = check("month", "2024-02-10", "2024-02-01", "2024-02-29")
    @Test fun sundayBelongsToPreviousMonday() = check("week", "2026-10-25", "2026-10-19", "2026-10-25")
    @Test fun weekCanCrossYearBoundary() = check("week", "2026-01-01", "2025-12-29", "2026-01-04")
    @Test fun singleDayDoesNotExpandRange() = check("day", "2026-10-25", "2026-10-25", "2026-10-25")
}
