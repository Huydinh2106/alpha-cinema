package com.example.alphacinema.data.model

data class CreateWatchPartyRoomRequest(
    val movieSlug: String,
    val movieTitle: String,
    val moviePosterUrl: String,
    val episodeId: String?,
    val episodeName: String?
)

data class JoinWatchPartyRoomRequest(
    val roomId: String
)

data class LeaveWatchPartyRoomRequest(
    val roomId: String
)

data class WatchPartyActionResponse(
    val roomId: String?,
    val maxMembers: Int?,
    val message: String?,
    val error: String?
)
