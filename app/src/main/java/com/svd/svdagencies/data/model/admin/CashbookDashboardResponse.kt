package com.svd.svdagencies.data.model.admin

import com.google.gson.annotations.SerializedName

data class CashbookDashboardResponse(
    val date: String,
    val cash_in: Double,
    val denominations: Denominations,
    val cash_out: Double,
    val bank_balance: Double,
    val company_dues: List<CompanyDue>,
    val total_company_dues: Double,
    val total_customer_dues: Double,
    val monthly_profit: Double,
    val net_profit: Double,
    val net_cash: Double,
    val stock_value: Double,
    val remaining_amount: Double
)

data class Denominations(
    val c500: Int,
    val c200: Int,
    val c100: Int,
    val c50: Int,
    val c20: Int,
    val c10: Int,
    val coin20: Int,
    val coin10: Int,
    val coin5: Int,
    val coin2: Int,
    val coin1: Int
)

data class CompanyDue(
    val company_name: String,
    val total_invoice: Double,
    val total_paid: Double,
    val total_due: Double,
    val last_updated: String
)
