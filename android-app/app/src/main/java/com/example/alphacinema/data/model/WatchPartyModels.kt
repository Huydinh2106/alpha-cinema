package com.example.alphacinema.data.model

data class WatchPartyRoom(
    val roomId: String = "",
    val hostId: String = "",
    val hostName: String = "",
    val movieSlug: String = "",
    val movieTitle: String = "",
    val moviePosterUrl: String = "",
    val episodeId: String? = null,
    val episodeName: String? = null,
    val playbackState: String = "paused", // "playing" | "paused"
    val currentTimeSec: Double = 0.0,     // video position at last state change
    val playStartedAt: Long = 0,          // server timestamp when play started
    val lastUpdated: Long = 0,
    val createdAt: Long = 0,
    val maxMembers: Int = 5
)

data class WatchPartyMember(
    val uid: String = "",
    val displayName: String = "",
    val photoUrl: String = ""
)

data class WatchPartyChatMessage(
    val id: String = "",
    val uid: String = "",
    val displayName: String = "",
    val text: String = "",
    val timestamp: Long = 0
)
