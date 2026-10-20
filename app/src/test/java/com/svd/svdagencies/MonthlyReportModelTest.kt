package com.svd.svdagencies

import com.google.gson.Gson
import com.svd.svdagencies.data.model.admin.customerData.MonthlySummaryResponse
import org.junit.Assert.*
import org.junit.Test

class MonthlyReportModelTest {
    @Test fun groupedStatementAcceptsServerAmountsAndMissingGst() {
        val json = """{"year":2026,"month":10,"days_in_month":31,"customer":{"id":1,"name":"Ravi","phone":"9999999999","shop_name":"Corner shop"},"summary":{"total_sales":"105.00","paid_amount":"20.00","opening_due":"0.00","due_amount":"85.00","remaining_due":"85.00","total_items":"1","total_discount":"0.00"},"volume":{"milk_volume":"0","curd_volume":"0.5","total_volume":"0.5","avg_milk_per_day":"0","avg_curd_per_day":"0.016","avg_total_per_day":"0.016"},"statement_groups":[{"date":"2026-10-20","total":"105.00","paid":"20.00","due":"85.00","lines":[{"item_code":"CURD500","item_name":"Curd 500 ml","quantity":"1","rate":"105.00","amount":"105.00","gst_rate":"5.00","gst_amount":"5.00"},{"item_code":"OLD","item_name":"Old snapshot","quantity":1,"rate":0,"amount":0,"gst_rate":null,"gst_amount":null}]}]}"""
        val report = Gson().fromJson(json, MonthlySummaryResponse::class.java)
        val group = report.statement_groups!!.single()
        assertEquals(105.0, group.total, 0.001)
        assertEquals(20.0, group.paid, 0.001)
        assertEquals(85.0, group.due, 0.001)
        assertEquals(5.0, group.lines!![0].gst_amount!!, 0.001)
        assertNull(group.lines[1].gst_rate)
        assertEquals("Corner shop", report.customer.shop_name)
    }
}
