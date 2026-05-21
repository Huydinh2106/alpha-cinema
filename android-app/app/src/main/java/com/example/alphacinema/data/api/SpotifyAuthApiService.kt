package com.example.alphacinema.data.api

import com.example.alphacinema.data.model.SpotifyTokenResponse
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Header
import retrofit2.http.POST

interface SpotifyAuthApiService {
    @FormUrlEncoded
    @POST("api/token")
    suspend fun requestClientCredentialsToken(
        @Header("Authorization") authorization: String,
        @Field("grant_type") grantType: String = "client_credentials"
    ): Response<SpotifyTokenResponse>
}
