package com.example.alphacinema.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

data class UserProfile(
    var uid: String = "",
    var email: String = "",
    var displayName: String = "",
    var photoUrl: String = "",
    var isAdmin: Boolean = false,
    var subscriptionPlan: String = "free",
    var subscriptionStatus: String = "inactive",
    var subscriptionStartedAt: Timestamp? = null,
    var subscriptionExpiresAt: Timestamp? = null,
    var subscriptionUpdatedAt: Timestamp? = null,
    @ServerTimestamp var createdAt: Timestamp? = null,
    @ServerTimestamp var updatedAt: Timestamp? = null
)

data class WatchHistoryItem(
    var movieId: String = "", // Same as slug
    val movieName: String = "",
    val originName: String = "",
    val posterUrl: String = "",
    val episodeId: String = "",
    val episodeName: String = "",
    val progress: Long = 0,
    val duration: Long = 0,
    @ServerTimestamp val lastWatchedAt: Timestamp? = null
)

data class FavoriteItem(
    var movieId: String = "", // Same as slug
    val movieName: String = "",
    val posterUrl: String = "",
    @ServerTimestamp val addedAt: Timestamp? = null
)

data class MovieStats(
    var movieId: String = "", // Same as slug
    val averageRating: Double = 0.0,
    val totalRatings: Long = 0,
    val totalComments: Long = 0
)

data class Comment(
    var id: String = "", // Auto-generated
    val userId: String = "",
    val userName: String = "",
    val userAvatar: String = "",
    val content: String = "",
    val parentCommentId: String = "",
    val replyToUserName: String = "",
    val likes: Long = 0,
    val likedBy: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class Rating(
    var userId: String = "",
    val score: Int = 0, // 1 to 10
    @ServerTimestamp val createdAt: Timestamp? = null
)

data class FirestoreMovie(
    var slug: String = "",
    var title: String = "",
    var originName: String = "",
    var type: String = "single",
    var status: String = "completed",
    var posterUrl: String = "",
    var thumbUrl: String = "",
    var year: Long = 0L,
    var content: String = "",
    var categories: List<String> = emptyList(),
    var countries: List<String> = emptyList(),
    var actors: List<String> = emptyList(),
    var directors: List<String> = emptyList(),
    var searchKeywords: List<String> = emptyList(),
    var ageRating: String = "13+",
    var tmdbVoteAverage: Double = 0.0,
    @get:com.google.firebase.firestore.PropertyName("isKidsFriendly")
    @set:com.google.firebase.firestore.PropertyName("isKidsFriendly")
    var isKidsFriendly: Boolean = false,
    @ServerTimestamp var modifiedTime: Timestamp? = null
)

data class HomeCategory(
    var id: String = "",
    var title: String = "",
    var movieSlugs: List<String> = emptyList()
)
