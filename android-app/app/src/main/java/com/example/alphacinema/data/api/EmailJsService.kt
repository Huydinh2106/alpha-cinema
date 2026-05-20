package com.example.alphacinema.data.api

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Data class gửi lên EmailJS REST API.
 */
data class EmailJsRequest(
    val service_id: String,
    val template_id: String,
    val user_id: String,       // Public Key từ EmailJS
    val template_params: Map<String, String>
)

/**
 * Retrofit interface cho EmailJS REST API.
 * EmailJS trả về plain text "OK" (không phải JSON), nên dùng ResponseBody.
 */
interface EmailJsService {
    @POST("api/v1.0/email/send")
    suspend fun sendEmail(@Body request: EmailJsRequest): Response<ResponseBody>
}
