package com.example.alphacinema.data.model

data class MomoPaymentRequest(
    val partnerCode: String,
    //val accessKey: String,
    val partnerName: String,
    val storeId: String,
    val requestId: String,
    val amount: Long,
    val orderId: String,
    val orderInfo: String,
    val redirectUrl: String,
    val ipnUrl: String,
    val extraData: String,
    val requestType: String,
    val signature: String,
    val lang: String = "vi"
)

data class MomoPaymentResponse(
    val partnerCode: String,
    val orderId: String,
    val requestId: String,
    val amount: Long,
    val responseTime: Long,
    val message: String,
    val resultCode: Int,
    val payUrl: String?,
    val deeplink: String?,
    val qrCodeUrl: String?,
    val signature: String?
)
