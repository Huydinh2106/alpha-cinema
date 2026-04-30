package com.example.alphacinema.data.repository

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
    private var cachedAllMovies: List<com.example.alphacinema.data.model.FirestoreMovie>? = null

    private fun String.removeAccents(): String {
        val normalized = java.text.Normalizer.normalize(this, java.text.Normalizer.Form.NFD)
        val pattern = java.util.regex.Pattern.compile("\\p{InCombiningDiacriticalMarks}+")
        return pattern.matcher(normalized).replaceAll("").replace("đ", "d").replace("Đ", "D")
    }

    suspend fun getUserProfile(uid: String): com.example.alphacinema.data.model.UserProfile? {
        if (uid.isBlank()) return null
        return try {
            val snapshot = db.collection("users").document(uid).get().await()
            snapshot.toObject(com.example.alphacinema.data.model.UserProfile::class.java)
        } catch (e: Exception) {
            android.util.Log.e("FirestoreRepository", "getUserProfile failed", e)
            null
        }
    }

    suspend fun checkIfAdmin(uid: String): Boolean {
        if (uid.isBlank()) return false
        return try {
            val snapshot = db.collection("users").document(uid).get().await()
            snapshot.getBoolean("isAdmin") == true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun saveMovie(movie: com.example.alphacinema.data.model.FirestoreMovie) {
        try {
            db.collection("movies").document(movie.slug).set(movie).await()
            cachedAllMovies = null // Invalidate cache
        } catch (e: Exception) {
            android.util.Log.e("FirestoreRepository", "saveMovie failed", e)
        }
    }

    suspend fun getMovieBySlug(slug: String): com.example.alphacinema.data.model.FirestoreMovie? {
        if (slug.isBlank()) return null
        return try {
            val snapshot = db.collection("movies").document(slug).get().await()
            snapshot.toObject(com.example.alphacinema.data.model.FirestoreMovie::class.java)
                ?.apply { this.slug = snapshot.id }
        } catch (e: Exception) {
            android.util.Log.e("FirestoreRepository", "getMovieBySlug failed for slug=$slug", e)
            null
        }
    }

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
                    val items = snapshot.documents.mapNotNull { doc -> 
                        doc.toObject(WatchHistoryItem::class.java)?.apply { this.movieId = doc.id } 
                    }
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
                    val items = snapshot.documents.mapNotNull { doc -> 
                        doc.toObject(FavoriteItem::class.java)?.apply { this.movieId = doc.id } 
                    }
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
                    val comments = snapshot.documents.mapNotNull { doc -> 
                        doc.toObject(Comment::class.java)?.apply { this.id = doc.id } 
                    }
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
                    trySend(snapshot.toObject(Rating::class.java)?.apply { this.userId = snapshot.id })
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
                    trySend(snapshot.toObject(MovieStats::class.java)?.apply { this.movieId = snapshot.id })
                } else {
                    trySend(null)
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun getMovies(isKidsMode: Boolean, limit: Int = 20): List<com.example.alphacinema.data.model.FirestoreMovie> {
        return try {
            var query: Query = db.collection("movies")
                .limit(limit.toLong())

            if (isKidsMode) {
                query = query.whereEqualTo("isKidsFriendly", true)
            }

            val snapshot = query.get().await()
            android.util.Log.i("FirestoreRepository", "getMovies retrieved ${snapshot.documents.size} movies")
            snapshot.documents.mapNotNull { doc ->
                try {
                    val m = doc.toObject(com.example.alphacinema.data.model.FirestoreMovie::class.java)
                    m?.apply { slug = doc.id }
                } catch (e: Exception) {
                    android.util.Log.e("FirestoreRepository", "Failed to deserialize doc: ${doc.id}", e)
                    null
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("FirestoreRepository", "getMovies failed", e)
            emptyList()
        }
    }

    suspend fun getMoviesByType(type: String, isKidsMode: Boolean, limit: Int = 20, excludeSlugs: Set<String> = emptySet()): List<com.example.alphacinema.data.model.FirestoreMovie> {
        return try {
            var query: Query = db.collection("movies")
                .whereEqualTo("type", type)
                .limit((limit + excludeSlugs.size).toLong())

            if (isKidsMode) {
                query = query.whereEqualTo("isKidsFriendly", true)
            }

            android.util.Log.i("FirestoreRepository", "getMoviesByType querying type=$type, limit=${limit + excludeSlugs.size}")
            val snapshot = query.get().await()
            android.util.Log.i("FirestoreRepository", "getMoviesByType retrieved ${snapshot.documents.size} docs for type=$type")
            snapshot.documents.mapNotNull { doc ->
                try {
                    val m = doc.toObject(com.example.alphacinema.data.model.FirestoreMovie::class.java)
                    m?.apply { slug = doc.id }
                } catch (e: Exception) {
                    android.util.Log.e("FirestoreRepository", "Failed to deserialize doc: ${doc.id}", e)
                    null
                }
            }.filter { it.slug !in excludeSlugs }.take(limit)
        } catch (e: Exception) {
            android.util.Log.e("FirestoreRepository", "getMoviesByType failed for type=$type", e)
            emptyList()
        }
    }

    suspend fun getMoviesByCountry(country: String, isKidsMode: Boolean, limit: Int = 20, excludeSlugs: Set<String> = emptySet()): List<com.example.alphacinema.data.model.FirestoreMovie> {
        return try {
            var query: Query = db.collection("movies")
                .whereArrayContains("countries", country)
                .limit((limit + excludeSlugs.size).toLong())

            if (isKidsMode) {
                query = query.whereEqualTo("isKidsFriendly", true)
            }

            android.util.Log.i("FirestoreRepository", "getMoviesByCountry querying country=$country, limit=${limit + excludeSlugs.size}")
            val snapshot = query.get().await()
            snapshot.documents.mapNotNull { doc ->
                try {
                    val m = doc.toObject(com.example.alphacinema.data.model.FirestoreMovie::class.java)
                    m?.apply { slug = doc.id }
                } catch (e: Exception) {
                    android.util.Log.e("FirestoreRepository", "Failed to deserialize doc: ${doc.id}", e)
                    null
                }
            }.filter { it.slug !in excludeSlugs }.take(limit)
        } catch (e: Exception) {
            android.util.Log.e("FirestoreRepository", "getMoviesByCountry failed for country=$country", e)
            emptyList()
        }
    }

    suspend fun getMoviesByCategory(category: String, isKidsMode: Boolean, limit: Int = 20, excludeSlugs: Set<String> = emptySet()): List<com.example.alphacinema.data.model.FirestoreMovie> {
        return try {
             var query: Query = db.collection("movies")
                .whereArrayContains("categories", category)
                .limit((limit + excludeSlugs.size).toLong())

            if (isKidsMode) {
                query = query.whereEqualTo("isKidsFriendly", true)
            }

            android.util.Log.i("FirestoreRepository", "getMoviesByCategory querying category=$category, limit=${limit + excludeSlugs.size}")
            val snapshot = query.get().await()
            android.util.Log.i("FirestoreRepository", "getMoviesByCategory retrieved ${snapshot.documents.size} docs for category=$category")
            snapshot.documents.mapNotNull { doc ->
                try {
                    val m = doc.toObject(com.example.alphacinema.data.model.FirestoreMovie::class.java)
                    m?.apply { slug = doc.id }
                } catch (e: Exception) {
                    android.util.Log.e("FirestoreRepository", "Failed to deserialize doc: ${doc.id}", e)
                    null
                }
            }.filter { it.slug !in excludeSlugs }.take(limit)
        } catch (e: Exception) {
            android.util.Log.e("FirestoreRepository", "getMoviesByCategory failed for category=$category", e)
            emptyList()
        }
    }

    suspend fun searchMovies(keyword: String, isKidsMode: Boolean, limit: Int = 20): List<com.example.alphacinema.data.model.FirestoreMovie> {
        if (keyword.isBlank()) return emptyList()
        val normalizedQuery = keyword.trim().lowercase().removeAccents()
        return try {
            if (cachedAllMovies == null) {
                val snapshot = db.collection("movies").get().await()
                cachedAllMovies = snapshot.documents.mapNotNull { doc ->
                    try {
                        val m = doc.toObject(com.example.alphacinema.data.model.FirestoreMovie::class.java)
                        m?.apply { slug = doc.id }
                    } catch (e: Exception) {
                        null
                    }
                }
            }

            var results = cachedAllMovies ?: emptyList()
            if (isKidsMode) {
                results = results.filter { it.isKidsFriendly }
            }

            results = results.filter { movie ->
                val titleNoAccents = movie.title.lowercase().removeAccents()
                val originNameNoAccents = movie.originName.lowercase().removeAccents()
                titleNoAccents.contains(normalizedQuery) || originNameNoAccents.contains(normalizedQuery)
            }

            results.take(limit)
        } catch (e: Exception) {
            android.util.Log.e("FirestoreRepository", "searchMovies failed", e)
            emptyList()
        }
    }
}
