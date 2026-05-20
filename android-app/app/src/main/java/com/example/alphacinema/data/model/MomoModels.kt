package com.example.alphacinema.data.model

/**
 * Request sent to our Cloud Function (NOT directly to MoMo).
 * The Cloud Function handles signature creation and MoMo API call.
 */
data class MomoPaymentRequest(
    val amount: Long,
    val orderInfo: String,
    val plan: String
)

data class MomoAppPaymentRequest(
    val orderId: String,
    val amount: Long,
    val orderInfo: String,
    val plan: String,
    val token: String,
    val phoneNumber: String,
    val env: String
)

/**
 * Response returned from MoMo (via Cloud Function).
 */
data class MomoPaymentResponse(
    val partnerCode: String?,
    val orderId: String?,
    val requestId: String?,
    val amount: Long?,
    val responseTime: Long?,
    val message: String?,
    val resultCode: Int?,
    val payUrl: String?,
    val deeplink: String?,
    val qrCodeUrl: String?,
    val signature: String?,
    // Error case from Cloud Function
    val error: String?
)

data class MomoAppPaymentResponse(
    val orderId: String?,
    val status: Int?,
    val resultCode: Int?,
    val message: String?,
    val amount: Long?,
    val transid: String?,
    val transId: String?,
    val signature: String?,
    val error: String?
)
