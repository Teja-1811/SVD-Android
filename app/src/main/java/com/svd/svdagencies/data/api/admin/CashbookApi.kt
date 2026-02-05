package com.svd.svdagencies.data.api.admin

import com.svd.svdagencies.data.model.admin.*
import com.svd.svdagencies.data.model.admin.Cashbook.CashbookDashboardResponse
import com.svd.svdagencies.data.model.admin.Cashbook.ExpenseListResponse
import com.svd.svdagencies.data.model.admin.Cashbook.ExpenseRequest
import com.svd.svdagencies.data.model.admin.Cashbook.SaveBankBalanceRequest
import com.svd.svdagencies.data.model.admin.Cashbook.SaveCashInRequest
import retrofit2.http.*

interface CashbookApi {

    @GET("api/cashbook/entries/")
    suspend fun getDashboardData(
        @Query("month") month: Int? = null,
        @Query("year") year: Int? = null
    ): CashbookDashboardResponse

    @POST("api/cashbook/save-cash/")
    suspend fun saveCashIn(@Body request: SaveCashInRequest): Map<String, Boolean>

    @POST("api/cashbook/save-bank/")
    suspend fun saveBankBalance(@Body request: SaveBankBalanceRequest): Map<String, Boolean>

    @POST("api/cashbook/add-expense/")
    suspend fun addExpense(@Body request: ExpenseRequest): Map<String, Boolean>

    @PUT("api/cashbook/edit-expense/{id}/")
    suspend fun editExpense(
        @Path("id") id: Int,
        @Body request: ExpenseRequest
    ): Map<String, Boolean>

    @GET("api/cashbook/expenses/")
    suspend fun getExpenses(
        @Query("start_date") startDate: String?,
        @Query("end_date") endDate: String?
    ): ExpenseListResponse

    @DELETE("api/cashbook/delete-expense/{id}/")
    suspend fun deleteExpense(@Path("id") id: Int): Map<String, Boolean>
}
