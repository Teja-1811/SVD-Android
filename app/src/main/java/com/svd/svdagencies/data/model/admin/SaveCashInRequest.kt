package com.svd.svdagencies.data.model.admin

data class SaveCashInRequest(
    val c500: Int = 0,
    val c200: Int = 0,
    val c100: Int = 0,
    val c50: Int = 0,
    val c20: Int = 0,
    val c10: Int = 0,
    val coin20: Int = 0,
    val coin10: Int = 0,
    val coin5: Int = 0,
    val coin2: Int = 0,
    val coin1: Int = 0
)
