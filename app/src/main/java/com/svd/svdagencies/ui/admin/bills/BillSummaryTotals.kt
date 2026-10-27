package com.svd.svdagencies.ui.admin.bills

import kotlin.math.ceil

internal object BillSummaryTotals {
    // The server rounds the invoice once; the existing balance retains its paise.
    fun grandTotal(invoiceSubtotal: Double, openingDue: Double): Double =
        ceil(invoiceSubtotal) + openingDue
}
