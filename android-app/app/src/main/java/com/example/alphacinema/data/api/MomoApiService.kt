package com.example.alphacinema.data.api

import com.example.alphacinema.data.model.MomoPaymentRequest
import com.example.alphacinema.data.model.MomoPaymentResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface MomoApiService {
    @POST("v2/gateway/api/create")
    suspend fun createPayment(@Body request: MomoPaymentRequest): Response<MomoPaymentResponse>
}
