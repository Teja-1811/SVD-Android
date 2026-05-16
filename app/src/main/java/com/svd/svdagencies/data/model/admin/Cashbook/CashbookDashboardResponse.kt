package com.svd.svdagencies.data.model.admin.Cashbook

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
    val remaining_amount: Double,
    val delivery_salary: DeliverySalarySummary? = null
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

data class DeliverySalarySummary(
    val agents: List<DeliverySalaryAgent> = emptyList(),
    val salary_earned: Double = 0.0,
    val salary_paid: Double = 0.0,
    val remaining_salary: Double = 0.0
)

data class DeliverySalaryAgent(
    val agent_id: Int,
    val agent_name: String? = null,
    val agent_phone: String? = null,
    val salary_earned: Double = 0.0,
    val salary_paid: Double = 0.0,
    val remaining_salary: Double = 0.0
)

data class DeliverySalaryPaymentRequest(
    val delivery_agent_id: Int,
    val amount: Double,
    val payment_date: String? = null,
    val notes: String? = null
)
