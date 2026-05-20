package com.example.alphacinema.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private const val BASE_URL = "https://phimapi.com/"
    private const val SUPPORT_CHAT_BASE_URL = "https://vankhoa2110-rag-alphacinema.hf.space/"
    private const val TMDB_BASE_URL = "https://api.themoviedb.org/3/"
    private const val EMAILJS_BASE_URL = "https://api.emailjs.com/"
    private const val FUNCTIONS_BASE_URL = "https://asia-southeast1-alpha-cinema-39dfb.cloudfunctions.net/"
    // MoMo payments go through Cloud Functions (no direct MoMo calls from app)

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private fun createRetrofit(baseUrl: String): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val instance: PhimApiService by lazy {
        createRetrofit(BASE_URL).create(PhimApiService::class.java)
    }

    val supportChatApi: SupportChatApiService by lazy {
        createRetrofit(SUPPORT_CHAT_BASE_URL).create(SupportChatApiService::class.java)
    }

    val tmdbApi: TmdbApiService by lazy {
        createRetrofit(TMDB_BASE_URL).create(TmdbApiService::class.java)
    }

    val emailJsApi: EmailJsService by lazy {
        createRetrofit(EMAILJS_BASE_URL).create(EmailJsService::class.java)
    }

    val functionsApi: FunctionsService by lazy {
        createRetrofit(FUNCTIONS_BASE_URL).create(FunctionsService::class.java)
    }

    val momoApi: MomoApiService by lazy {
        createRetrofit(FUNCTIONS_BASE_URL).create(MomoApiService::class.java)
    }
}

interface FunctionsService {
    @retrofit2.http.POST("resetPasswordAdmin")
    suspend fun resetPassword(@retrofit2.http.Body body: Map<String, String>): retrofit2.Response<okhttp3.ResponseBody>

    @retrofit2.http.POST("getCustomToken")
    suspend fun getCustomToken(@retrofit2.http.Body body: Map<String, String>): retrofit2.Response<Map<String, String>>
}
