package com.example.alphacinema.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

data class UserProfile(
    @DocumentId val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val photoUrl: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class WatchHistoryItem(
    @DocumentId val movieId: String = "", // Same as slug
    val movieName: String = "",
    val posterUrl: String = "",
    val episodeId: String = "",
    val episodeName: String = "",
    val progress: Long = 0,
    val duration: Long = 0,
    @ServerTimestamp val lastWatchedAt: Timestamp? = null
)

data class FavoriteItem(
    @DocumentId val movieId: String = "", // Same as slug
    val movieName: String = "",
    val posterUrl: String = "",
    @ServerTimestamp val addedAt: Timestamp? = null
)

data class MovieStats(
    @DocumentId val movieId: String = "", // Same as slug
    val averageRating: Double = 0.0,
    val totalRatings: Long = 0,
    val totalComments: Long = 0
)

data class Comment(
    @DocumentId val id: String = "", // Auto-generated
    val userId: String = "",
    val userName: String = "",
    val userAvatar: String = "",
    val content: String = "",
    val likes: Long = 0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class Rating(
    @DocumentId val userId: String = "",
    val score: Int = 0, // 1 to 10
    @ServerTimestamp val createdAt: Timestamp? = null
)
