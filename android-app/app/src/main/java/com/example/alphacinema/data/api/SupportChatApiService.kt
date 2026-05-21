package com.example.alphacinema.data.api

import com.example.alphacinema.data.model.SupportChatRequest
import com.example.alphacinema.data.model.SupportRecommendRequest
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface SupportChatApiService {
    @POST("ask")
    suspend fun askQuestion(
        @Body request: SupportChatRequest
    ): Response<ResponseBody>

    @POST("recommend")
    suspend fun recommend(
        @Body request: SupportRecommendRequest
    ): Response<ResponseBody>
}
