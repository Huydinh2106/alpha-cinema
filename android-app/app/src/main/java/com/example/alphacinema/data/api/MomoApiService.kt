package com.example.alphacinema.data.api

import com.example.alphacinema.data.model.MomoPaymentRequest
import com.example.alphacinema.data.model.MomoPaymentResponse
import com.example.alphacinema.data.model.MomoAppPaymentRequest
import com.example.alphacinema.data.model.MomoAppPaymentResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * Calls our Firebase Cloud Function, which handles
 * MoMo signature creation and API call server-side.
 */
interface MomoApiService {
    @POST("createMomoPayment")
    suspend fun createPayment(
        @Header("Authorization") authorization: String,
        @Body request: MomoPaymentRequest
    ): Response<MomoPaymentResponse>

    @POST("processMomoAppPayment")
    suspend fun processAppPayment(
        @Header("Authorization") authorization: String,
        @Body request: MomoAppPaymentRequest
    ): Response<MomoAppPaymentResponse>
}
