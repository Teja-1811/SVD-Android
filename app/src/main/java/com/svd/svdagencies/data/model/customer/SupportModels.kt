package com.svd.svdagencies.data.model.customer

import com.google.gson.annotations.SerializedName

data class SupportTicketSummaryResponse(
    val success: Boolean,
    @SerializedName("raised_tickets")
    val raisedTickets: Int = 0,
    @SerializedName("resolved_tickets")
    val resolvedTickets: Int = 0,
    @SerializedName("total_tickets")
    val totalTickets: Int = 0
)

data class CustomerContactResponse(
    val success: Boolean,
    val message: String,
    @SerializedName("contact_id")
    val contactId: Int? = null,
    val status: String? = null
)

data class RaisedQueriesResponse(
    val success: Boolean,
    val count: Int = 0,
    val queries: List<RaisedQuery> = emptyList()
)

data class RaisedQuery(
    val id: Int,
    val name: String,
    val phone: String,
    val email: String = "",
    val subject: String,
    val message: String,
    val status: String,
    @SerializedName("created_at")
    val createdAt: String
)
