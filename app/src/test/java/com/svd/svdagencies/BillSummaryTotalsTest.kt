package com.svd.svdagencies

import com.svd.svdagencies.ui.admin.bills.BillSummaryTotals
import org.junit.Assert.assertEquals
import org.junit.Test

class BillSummaryTotalsTest {
    @Test fun addsOpeningDueToRoundedInvoice() {
        assertEquals(1384.0, BillSummaryTotals.grandTotal(384.0, 1000.0), 0.000001)
    }
    @Test fun roundsInvoiceBeforeAddingFractionalOpeningDue() {
        assertEquals(42.5, BillSummaryTotals.grandTotal(31.2, 10.5), 0.000001)
    }
    @Test fun preservesCreditBalances() {
        assertEquals(21.5, BillSummaryTotals.grandTotal(31.2, -10.5), 0.000001)
    }
    @Test fun noOpeningDueStillShowsInvoiceAmount() {
        assertEquals(32.0, BillSummaryTotals.grandTotal(31.2, 0.0), 0.000001)
    }
    @Test fun noItemsShowsExistingBalance() {
        assertEquals(100.5, BillSummaryTotals.grandTotal(0.0, 100.5), 0.000001)
    }
}
