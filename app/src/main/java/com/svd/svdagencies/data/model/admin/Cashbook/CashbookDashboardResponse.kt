package com.svd.svdagencies.data.model.admin.Cashbook

import com.google.gson.annotations.SerializedName

data class CashbookDashboardResponse(
    val success: Boolean,
    val summary: CashbookSummary,
    @SerializedName("cash_entry")
    val cashEntry: CashEntry,
    val expenses: List<CashbookExpense>,
    @SerializedName("salary_payments")
    val salaryPayments: List<SalaryPaymentRecord>,
    @SerializedName("company_dues")
    val companyDues: List<CompanyDue>,
    @SerializedName("delivery_salary")
    val deliverySalary: DeliverySalarySummary?,
    @SerializedName("commission_credits")
    val commissionCredits: List<CommissionCredit>,
    @SerializedName("leakage_entries")
    val leakageEntries: List<LeakageEntryRecord>,
    val statement: StatementInfo,
    val filters: CashbookFilters
)

data class CashbookSummary(
    val cash_in: Double,
    val cash_out: Double,
    val bank_balance: Double,
    val opening_balance: Double,
    val net_cash: Double,
    val monthly_profit: Double,
    val net_profit: Double,
    val monthly_loss: Double,
    val stock_value: Double,
    val remaining_amount: Double,
    val company_due: Double,
    val customer_due: Double,
    val salary_paid: Double,
    val commission_credit: Double
)

data class CashEntry(
    val id: Int,
    val total: Double,
    val denominations: List<DenominationItem>
)

data class DenominationItem(
    val name: String,
    val count: Int,
    val amount: Double
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

data class CashbookExpense(
    val id: Int,
    val date: String,
    val category: String,
    val amount: Double,
    val description: String
)

data class SalaryPaymentRecord(
    val id: Int,
    val agent: String,
    val amount: Double,
    val payment_date: String,
    val notes: String?
)

data class CompanyDue(
    @SerializedName("company_id")
    val companyId: Int,
    @SerializedName("company_name")
    val companyName: String,
    val invoice: Double,
    val paid: Double,
    val due: Double,
    val last_updated: String?
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

data class CommissionCredit(
    val id: Int,
    val company: String,
    @SerializedName("company_id")
    val companyId: Int,
    val amount: Double,
    val description: String,
    @SerializedName("created_at")
    val createdAt: String
)

data class LeakageEntryRecord(
    val id: Int,
    val date: String,
    val item: String,
    val company: String,
    val quantity: Int,
    @SerializedName("unit_cost")
    val unitCost: Double,
    val loss: Double,
    val notes: String?
)

data class StatementInfo(
    val opening_balance: Double,
    val closing_balance: Double,
    val start_date: String,
    val end_date: String
)

data class CashbookFilters(
    val months: List<MonthOption>,
    val years: List<Int>,
    val selected_month: Int,
    val selected_year: Int,
    val companies: List<CompanyOption>,
    @SerializedName("expense_categories")
    val expenseCategories: List<ExpenseCategoryOption>
)

data class MonthOption(val id: Int, val name: String)
data class CompanyOption(val id: Int, val name: String)
data class ExpenseCategoryOption(val value: String, val label: String)

data class DeliverySalaryPaymentRequest(
    val delivery_agent_id: Int,
    val amount: Double,
    val payment_date: String? = null,
    val notes: String? = null
)
