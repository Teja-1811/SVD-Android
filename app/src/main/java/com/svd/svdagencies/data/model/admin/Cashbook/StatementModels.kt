package com.svd.svdagencies.data.model.admin.Cashbook

data class StatementResponse(
    val month: Int,
    val year: Int,
    val start_date: String,
    val end_date: String,
    val summary: StatementSummary,
    val pdf_url: String? = null,
    val overall_summary: List<StatementSummaryRow> = emptyList(),
    val profit_loss_summary: List<StatementSummaryRow> = emptyList(),
    val transactions: List<StatementTransaction> = emptyList()
)

data class StatementSummary(
    val opening_balance: Double = 0.0,
    val total_credit: Double = 0.0,
    val total_debit: Double = 0.0,
    val closing_balance: Double = 0.0,
    val transaction_count: Int = 0
)

data class StatementTransaction(
    val id: Int,
    val date: String,
    val particulars: String,
    val reference: String? = null,
    val type: String,
    val type_label: String,
    val direction: String,
    val withdrawal: Double = 0.0,
    val deposit: Double = 0.0,
    val balance: Double = 0.0
)

data class StatementSummaryRow(
    val particulars: String,
    val amount: Double = 0.0
)
