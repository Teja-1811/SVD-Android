package com.svd.svdagencies.data.api.customer

import com.svd.svdagencies.data.model.customer.InvoiceResponse
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface InvoiceApi {

    @GET("api/customer/invoices/")
    fun getCustomerInvoices(
        @Query("month") month: String,
        @Query("year") year: String
    ): Call<InvoiceResponse>

    @GET("api/customer/invoice/download/")
    fun downloadInvoice(
        @Query("invoice_number") invoiceNumber: String
    ): Call<ResponseBody>

}