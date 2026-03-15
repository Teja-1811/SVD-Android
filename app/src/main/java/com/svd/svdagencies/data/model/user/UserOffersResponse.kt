package com.svd.svdagencies.data.model.user

data class UserOffersResponse(
    val status: Boolean,
    val offers: List<UserOffer> = emptyList()
)
