package com.svd.svdagencies.data.api.admin

import com.svd.svdagencies.data.model.admin.*
import retrofit2.http.*

interface CashbookApi {

    @GET("cashbook/entries/")
    suspend fun getDashboardData(): CashbookDashboardResponse

    @POST("cashbook/save-cash/")
    suspend fun saveCashIn(@Body request: SaveCashInRequest): Map<String, Boolean>

    @POST("cashbook/save-bank/")
    suspend fun saveBankBalance(@Body request: SaveBankBalanceRequest): Map<String, Boolean>

    @POST("cashbook/add-expense/")
    suspend fun addExpense(@Body request: ExpenseRequest): Map<String, Boolean>

    @PUT("cashbook/edit-expense/{id}/")
    suspend fun editExpense(
        @Path("id") id: Int,
        @Body request: ExpenseRequest
    ): Map<String, Boolean>

    @GET("cashbook/expenses/")
    suspend fun getExpenses(
        @Query("start_date") startDate: String?,
        @Query("end_date") endDate: String?
    ): ExpenseListResponse

    @DELETE("cashbook/delete-expense/{id}/")
    suspend fun deleteExpense(@Path("id") id: Int): Map<String, Boolean>
}
