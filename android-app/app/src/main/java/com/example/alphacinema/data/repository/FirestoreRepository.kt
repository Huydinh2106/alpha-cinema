package com.example.alphacinema.data.repository

import com.example.alphacinema.MovieUi
import com.example.alphacinema.data.model.Comment
import com.example.alphacinema.data.model.FavoriteItem
import com.example.alphacinema.data.model.MovieStats
import com.example.alphacinema.data.model.Rating
import com.example.alphacinema.data.model.UserProfile
import com.example.alphacinema.data.model.WatchHistoryItem
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreRepository {
    private val db = FirebaseFirestore.getInstance()

    suspend fun saveUser(firebaseUser: FirebaseUser) {
        val userRef = db.collection("users").document(firebaseUser.uid)
        
        // We only create the user document if it doesn't exist, to avoid overwriting createdAt
        val snapshot = userRef.get().await()
        if (!snapshot.exists()) {
            val userProfile = UserProfile(
                uid = firebaseUser.uid,
                email = firebaseUser.email ?: "",
                displayName = firebaseUser.displayName ?: "",
                photoUrl = firebaseUser.photoUrl?.toString() ?: ""
                // createdAt and updatedAt will be automatically set by @ServerTimestamp
            )
            userRef.set(userProfile).await()
        } else {
            // Update displayName and photoUrl if needed
            userRef.update(
                mapOf(
                    "displayName" to (firebaseUser.displayName ?: ""),
                    "photoUrl" to (firebaseUser.photoUrl?.toString() ?: "")
                )
            ).await()
        }
    }

    suspend fun updateWatchProgress(
        userId: String,
        movieSlug: String,
        movieName: String,
        posterUrl: String,
        episodeId: String,
        episodeName: String,
        progress: Long,
        duration: Long
    ) {
        if (userId.isBlank() || movieSlug.isBlank()) return
        val historyRef = db.collection("users").document(userId)
            .collection("watch_history").document(movieSlug)

        val historyItem = WatchHistoryItem(
            movieId = movieSlug,
            movieName = movieName,
            posterUrl = posterUrl,
            episodeId = episodeId,
            episodeName = episodeName,
            progress = progress,
            duration = duration
        )
        historyRef.set(historyItem).await()
    }

    fun getWatchHistory(userId: String): Flow<List<WatchHistoryItem>> = callbackFlow {
        if (userId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = db.collection("users").document(userId)
            .collection("watch_history")
            .orderBy("lastWatchedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val items = snapshot.documents.mapNotNull { it.toObject(WatchHistoryItem::class.java) }
                    trySend(items)
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun toggleFavorite(
        userId: String,
        movieSlug: String,
        movieName: String,
        posterUrl: String,
        isFavorite: Boolean
    ) {
        if (userId.isBlank() || movieSlug.isBlank()) return
        val favRef = db.collection("users").document(userId)
            .collection("favorites").document(movieSlug)

        if (isFavorite) {
            val favItem = FavoriteItem(
                movieId = movieSlug,
                movieName = movieName,
                posterUrl = posterUrl
            )
            favRef.set(favItem).await()
        } else {
            favRef.delete().await()
        }
    }

    fun getFavorites(userId: String): Flow<List<FavoriteItem>> = callbackFlow {
        if (userId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = db.collection("users").document(userId)
            .collection("favorites")
            .orderBy("addedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val items = snapshot.documents.mapNotNull { it.toObject(FavoriteItem::class.java) }
                    trySend(items)
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun postComment(
        movieId: String,
        userId: String,
        userName: String,
        userAvatar: String,
        content: String
    ) {
        if (movieId.isBlank() || userId.isBlank() || content.isBlank()) return
        
        val commentsRef = db.collection("movies").document(movieId)
            .collection("comments")
            
        val comment = Comment(
            userId = userId,
            userName = userName,
            userAvatar = userAvatar,
            content = content
        )
        commentsRef.add(comment).await()
    }

    fun getComments(movieId: String): Flow<List<Comment>> = callbackFlow {
        if (movieId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = db.collection("movies").document(movieId)
            .collection("comments")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val comments = snapshot.documents.mapNotNull { it.toObject(Comment::class.java) }
                    trySend(comments)
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun submitRating(movieId: String, userId: String, score: Int) {
        if (movieId.isBlank() || userId.isBlank() || score !in 1..10) return
        
        val ratingRef = db.collection("movies").document(movieId)
            .collection("ratings").document(userId)
            
        val rating = Rating(
            userId = userId,
            score = score
        )
        ratingRef.set(rating).await()
    }

    fun getUserRating(movieId: String, userId: String): Flow<Rating?> = callbackFlow {
        if (movieId.isBlank() || userId.isBlank()) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val listener = db.collection("movies").document(movieId)
            .collection("ratings").document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    trySend(snapshot.toObject(Rating::class.java))
                } else {
                    trySend(null)
                }
            }
        awaitClose { listener.remove() }
    }

    fun getMovieStats(movieId: String): Flow<MovieStats?> = callbackFlow {
        if (movieId.isBlank()) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val listener = db.collection("movies").document(movieId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    trySend(snapshot.toObject(MovieStats::class.java))
                } else {
                    trySend(null)
                }
            }
        awaitClose { listener.remove() }
    }
}
