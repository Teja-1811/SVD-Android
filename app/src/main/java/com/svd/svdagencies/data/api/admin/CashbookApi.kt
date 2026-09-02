package com.svd.svdagencies.data.api.admin

import com.svd.svdagencies.data.model.admin.*
import com.svd.svdagencies.data.model.admin.Cashbook.CashbookDashboardResponse
import com.svd.svdagencies.data.model.admin.Cashbook.DeliverySalaryPaymentRequest
import com.svd.svdagencies.data.model.admin.Cashbook.ExpenseListResponse
import com.svd.svdagencies.data.model.admin.Cashbook.ExpenseRequest
import com.svd.svdagencies.data.model.admin.Cashbook.SaveBankBalanceRequest
import com.svd.svdagencies.data.model.admin.Cashbook.SaveCashInRequest
import com.svd.svdagencies.data.model.admin.Cashbook.StatementResponse
import okhttp3.ResponseBody
import retrofit2.http.*

interface CashbookApi {

    @Headers("Cache-Control: no-cache")
    @GET("api/cashbook/entries/")
    suspend fun getDashboardData(
        @Query("month") month: Int? = null,
        @Query("year") year: Int? = null
    ): CashbookDashboardResponse

    @POST("api/cashbook/save-cash/")
    suspend fun saveCashIn(@Body request: SaveCashInRequest): Map<String, Any>

    @POST("api/cashbook/save-bank/")
    suspend fun saveBankBalance(@Body request: SaveBankBalanceRequest): Map<String, Any>

    @Headers("Cache-Control: no-cache")
    @GET("api/cashbook/statement/")
    suspend fun getStatement(
        @Query("month") month: Int? = null,
        @Query("year") year: Int? = null
    ): StatementResponse

    @GET("api/cashbook/statement/pdf/")
    suspend fun downloadStatementPdf(
        @Query("month") month: Int? = null,
        @Query("year") year: Int? = null
    ): ResponseBody

    @POST("api/cashbook/add-expense/")
    suspend fun addExpense(@Body request: ExpenseRequest): Map<String, Any>

    @POST("api/cashbook/delivery-agent-salary/")
    suspend fun addDeliveryAgentSalary(@Body request: DeliverySalaryPaymentRequest): Map<String, Any>

    @PUT("api/cashbook/edit-expense/{id}/")
    suspend fun editExpense(
        @Path("id") id: Int,
        @Body request: ExpenseRequest
    ): Map<String, Any>

    @GET("api/cashbook/expenses/")
    suspend fun getExpenses(
        @Query("start_date") startDate: String?,
        @Query("end_date") endDate: String?
    ): ExpenseListResponse

    @DELETE("api/cashbook/delete-expense/{id}/")
    suspend fun deleteExpense(@Path("id") id: Int): Map<String, Any>

    @POST("api/cashbook/commission-credits/add/")
    suspend fun addCommissionCredit(@Body request: Map<String, Any>): Map<String, Any>

    @POST("api/cashbook/commission-credits/{id}/edit/")
    suspend fun editCommissionCredit(
        @Path("id") id: Int,
        @Body request: Map<String, Any>
    ): Map<String, Any>

    @DELETE("api/cashbook/commission-credits/{id}/delete/")
    suspend fun deleteCommissionCredit(@Path("id") id: Int): Map<String, Any>
}
