package com.svd.svdagencies.data.model.customer

data class GenericResponse(
    val status: String,
    val new_balance: String? = null,
    val error: String? = null
)