package com.example.alphacinema.data.api

import com.example.alphacinema.data.model.CreateWatchPartyRoomRequest
import com.example.alphacinema.data.model.JoinWatchPartyRoomRequest
import com.example.alphacinema.data.model.LeaveWatchPartyRoomRequest
import com.example.alphacinema.data.model.WatchPartyActionResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface WatchPartyApiService {
    @POST("createWatchPartyRoom")
    suspend fun createRoom(
        @Header("Authorization") authorization: String,
        @Body request: CreateWatchPartyRoomRequest
    ): Response<WatchPartyActionResponse>

    @POST("joinWatchPartyRoom")
    suspend fun joinRoom(
        @Header("Authorization") authorization: String,
        @Body request: JoinWatchPartyRoomRequest
    ): Response<WatchPartyActionResponse>

    @POST("leaveWatchPartyRoom")
    suspend fun leaveRoom(
        @Header("Authorization") authorization: String,
        @Body request: LeaveWatchPartyRoomRequest
    ): Response<WatchPartyActionResponse>
}
