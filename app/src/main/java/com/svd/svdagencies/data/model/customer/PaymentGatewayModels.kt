package com.svd.svdagencies.data.model.customer

import com.google.gson.annotations.SerializedName
import com.svd.svdagencies.data.model.delivery.DeliveryCustomerPaymentRecord

data class PaymentGatewayInitResponse(
    val success: Boolean? = null,
    val gateway: String? = null,
    @SerializedName("payment_for") val paymentFor: String? = null,
    @SerializedName("payment_order_id") val paymentOrderId: String? = null,
    @SerializedName("invoice_number") val invoiceNumber: String? = null,
    @SerializedName("bill_id") val billId: Int? = null,
    val amount: Double? = null,
    @SerializedName("redirect_url") val redirectUrl: String? = null,
    @SerializedName("phonepe_order_id") val phonePeOrderId: String? = null,
    @SerializedName("upi_id") val upiId: String? = null,
    @SerializedName("upi_uri") val upiUri: String? = null,
    @SerializedName("qr_payload") val qrPayload: String? = null,
    @SerializedName("payment_note") val paymentNote: String? = null,
    @SerializedName("record_update_message") val recordUpdateMessage: String? = null,
    @SerializedName("verification_status") val verificationStatus: String? = null,
    @SerializedName("payment_history") val paymentHistory: List<DeliveryCustomerPaymentRecord>? = null,
    val message: String? = null,
    val error: String? = null
)

