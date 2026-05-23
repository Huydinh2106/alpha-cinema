package com.example.alphacinema.data.repository

import com.example.alphacinema.data.model.Comment
import com.example.alphacinema.data.model.FavoriteItem
import com.example.alphacinema.data.model.MovieStats
import com.example.alphacinema.data.model.PlaylistMovieItem
import com.example.alphacinema.data.model.Rating
import com.example.alphacinema.data.model.UserPlaylist
import com.example.alphacinema.data.model.WatchHistoryItem
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Calendar

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
        } catch (e: CancellationException) {
            throw e
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
        if (firebaseUser.uid.isBlank()) return
        val userRef = db.collection("users").document(firebaseUser.uid)
        try {
            val snapshot = userRef.get().await()
            val existingData = snapshot.data.orEmpty()
            val userData = mutableMapOf<String, Any>(
                "uid" to firebaseUser.uid,
                "email" to (firebaseUser.email ?: ""),
                "displayName" to (firebaseUser.displayName ?: ""),
                "photoUrl" to (firebaseUser.photoUrl?.toString() ?: ""),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            if (!snapshot.exists() || !existingData.containsKey("createdAt")) {
                userData["createdAt"] = FieldValue.serverTimestamp()
            }
            if (!existingData.containsKey("subscriptionPlan")) {
                userData["subscriptionPlan"] = "free"
            }
            if (!existingData.containsKey("subscriptionStatus")) {
                userData["subscriptionStatus"] = "inactive"
            }
            if (!existingData.containsKey("notified3Days")) {
                userData["notified3Days"] = false
            }
            if (!existingData.containsKey("notified1Day")) {
                userData["notified1Day"] = false
            }
            if (!existingData.containsKey("notifiedExpired")) {
                userData["notifiedExpired"] = false
            }


            userRef.set(userData, SetOptions.merge()).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            android.util.Log.e("FirestoreRepository", "saveUser failed", e)
        }
    }

    suspend fun updateUserSubscription(
        uid: String,
        plan: String,
        paymentMethod: String? = null,
        durationMonths: Int = 1
    ): Timestamp? {
        if (uid.isBlank()) return null
        return try {
            val userRef = db.collection("users").document(uid)
            val snapshot = userRef.get().await()
            val existingData = snapshot.data.orEmpty()
            val normalizedPlan = plan.trim().lowercase().ifBlank { "free" }
            val isPaidPlan = normalizedPlan != "free"
            val startedAt = Timestamp.now()
            val expiresAt = if (isPaidPlan) calculateSubscriptionExpiry(durationMonths) else null
            val updates = mutableMapOf<String, Any>(
                "uid" to uid,
                "subscriptionPlan" to normalizedPlan,
                "subscriptionStatus" to if (isPaidPlan) "active" else "inactive",
                "subscriptionUpdatedAt" to startedAt,
                "updatedAt" to FieldValue.serverTimestamp()
            )

            if (!snapshot.exists() || !existingData.containsKey("createdAt")) {
                updates["createdAt"] = FieldValue.serverTimestamp()
            }
            if (isPaidPlan && expiresAt != null) {
                updates["subscriptionStartedAt"] = startedAt
                updates["subscriptionExpiresAt"] = expiresAt
                updates["notified3Days"] = false
                updates["notified1Day"] = false
                updates["notifiedExpired"] = false
            } else {
                updates["subscriptionStartedAt"] = FieldValue.delete()
                updates["subscriptionExpiresAt"] = FieldValue.delete()
                updates["notified3Days"] = false
                updates["notified1Day"] = false
                updates["notifiedExpired"] = false

            }
            if (!paymentMethod.isNullOrBlank()) {
                updates["subscriptionPaymentMethod"] = paymentMethod
            }

            userRef.set(updates, SetOptions.merge()).await()
            expiresAt
        } catch (e: Exception) {
            android.util.Log.e("FirestoreRepository", "updateUserSubscription failed", e)
            null
        }
    }

    private fun calculateSubscriptionExpiry(durationMonths: Int): Timestamp {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MONTH, durationMonths.coerceAtLeast(1))
        return Timestamp(calendar.time)
    }

    suspend fun updateUserDisplayName(uid: String, displayName: String) {
        if (uid.isBlank()) return
        try {
            db.collection("users").document(uid)
                .set(mapOf("displayName" to displayName), com.google.firebase.firestore.SetOptions.merge()).await()
        } catch (e: Exception) {
            android.util.Log.e("FirestoreRepository", "updateUserDisplayName failed", e)
        }
    }

    suspend fun updateUserPhotoUrl(uid: String, photoUrl: String) {
        if (uid.isBlank()) return
        try {
            db.collection("users").document(uid)
                .set(mapOf("photoUrl" to photoUrl), com.google.firebase.firestore.SetOptions.merge()).await()
        } catch (e: Exception) {
            android.util.Log.e("FirestoreRepository", "updateUserPhotoUrl failed", e)
        }
    }

    suspend fun updateWatchProgress(
        userId: String,
        movieSlug: String,
        movieName: String,
        originName: String,
        posterUrl: String,
        episodeId: String,
        episodeName: String,
        progress: Long,
        duration: Long,
        isKidsMode: Boolean = false
    ) {
        if (userId.isBlank() || movieSlug.isBlank()) return
        val historyRef = db.collection("users").document(userId)
            .collection(WATCH_HISTORY_COLLECTION)
            .document(watchHistoryDocumentId(movieSlug, isKidsMode))

        val historyItem = mapOf(
            "movieId" to movieSlug,
            "movieName" to movieName,
            "originName" to originName,
            "posterUrl" to posterUrl,
            "episodeId" to episodeId,
            "episodeName" to episodeName,
            "progress" to progress,
            "duration" to duration,
            "profileMode" to if (isKidsMode) WATCH_HISTORY_MODE_KIDS else WATCH_HISTORY_MODE_DEFAULT,
            "lastWatchedAt" to FieldValue.serverTimestamp()
        )
        historyRef.set(historyItem, com.google.firebase.firestore.SetOptions.merge()).await()
    }

    fun getWatchHistory(userId: String, isKidsMode: Boolean = false): Flow<List<WatchHistoryItem>> = callbackFlow {
        if (userId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = db.collection("users").document(userId)
            .collection(WATCH_HISTORY_COLLECTION)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.w("FirestoreRepository", "getWatchHistory failed", error)
                    trySend(emptyList())
                    close()
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val items = snapshot.documents
                        .filter { doc -> isKidsWatchHistoryDocument(doc.id) == isKidsMode }
                        .mapNotNull { doc ->
                            doc.toObject(WatchHistoryItem::class.java)?.apply {
                                movieId = watchHistoryMovieId(doc.id, movieId)
                            }
                        }
                        .sortedByDescending { item -> item.lastWatchedAt?.toDate()?.time ?: 0L }
                    trySend(items)
                }
            }
        awaitClose { listener.remove() }
    }

    private fun watchHistoryDocumentId(movieSlug: String, isKidsMode: Boolean): String {
        return if (isKidsMode) "$KIDS_WATCH_HISTORY_DOCUMENT_PREFIX$movieSlug" else movieSlug
    }

    private fun isKidsWatchHistoryDocument(documentId: String): Boolean {
        return documentId.startsWith(KIDS_WATCH_HISTORY_DOCUMENT_PREFIX)
    }

    private fun watchHistoryMovieId(documentId: String, storedMovieId: String): String {
        return if (isKidsWatchHistoryDocument(documentId)) {
            storedMovieId.ifBlank { documentId.removePrefix(KIDS_WATCH_HISTORY_DOCUMENT_PREFIX) }
        } else {
            documentId
        }
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

    fun getPlaylists(userId: String): Flow<List<UserPlaylist>> = callbackFlow {
        if (userId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = db.collection("users").document(userId)
            .collection(PLAYLISTS_COLLECTION)
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.w("FirestoreRepository", "getPlaylists failed", error)
                    trySend(emptyList())
                    close()
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val items = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(UserPlaylist::class.java)?.apply { id = doc.id }
                    }
                    trySend(items)
                }
            }
        awaitClose { listener.remove() }
    }

    fun getPlaylistItems(
        userId: String,
        playlistId: String
    ): Flow<List<PlaylistMovieItem>> = callbackFlow {
        if (userId.isBlank() || playlistId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = playlistItemsRef(userId, playlistId)
            .orderBy("addedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.w("FirestoreRepository", "getPlaylistItems failed", error)
                    trySend(emptyList())
                    close()
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val items = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(PlaylistMovieItem::class.java)?.apply { movieId = doc.id }
                    }
                    trySend(items)
                }
            }
        awaitClose { listener.remove() }
    }

    fun getPlaylistIdsForMovie(
        userId: String,
        movieSlug: String
    ): Flow<Set<String>> = callbackFlow {
        if (userId.isBlank() || movieSlug.isBlank()) {
            trySend(emptySet())
            close()
            return@callbackFlow
        }

        val listener = db.collection("users").document(userId)
            .collection(PLAYLISTS_COLLECTION)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.w("FirestoreRepository", "getPlaylistIdsForMovie failed", error)
                    trySend(emptySet())
                    close()
                    return@addSnapshotListener
                }
                val playlistIds = snapshot?.documents?.map { it.id }.orEmpty()
                launch {
                    try {
                        val matchingIds = playlistIds.mapNotNull { playlistId ->
                            val item = playlistItemsRef(userId, playlistId)
                                .document(movieSlug)
                                .get()
                                .await()
                            playlistId.takeIf { item.exists() }
                        }.toSet()
                        trySend(matchingIds)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        android.util.Log.w(
                            "FirestoreRepository",
                            "getPlaylistIdsForMovie item lookup failed",
                            e
                        )
                        trySend(emptySet())
                    }
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun createPlaylist(
        userId: String,
        name: String,
        firstMovie: PlaylistMovieItem? = null
    ): String {
        if (userId.isBlank()) return ""
        val normalizedName = name.trim()
        if (normalizedName.isBlank()) return ""

        val playlistRef = db.collection("users").document(userId)
            .collection(PLAYLISTS_COLLECTION)
            .document()
        val playlistData = mapOf(
            "name" to normalizedName,
            "coverPosterUrl" to (firstMovie?.posterUrl.orEmpty()),
            "itemCount" to if (firstMovie != null) 1L else 0L,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        val batch = db.batch()
        batch.set(playlistRef, playlistData)
        if (firstMovie != null && firstMovie.movieId.isNotBlank()) {
            batch.set(
                playlistRef.collection(PLAYLIST_ITEMS_COLLECTION).document(firstMovie.movieId),
                mapOf(
                    "movieId" to firstMovie.movieId,
                    "movieName" to firstMovie.movieName,
                    "posterUrl" to firstMovie.posterUrl,
                    "addedAt" to FieldValue.serverTimestamp()
                )
            )
        }
        batch.commit().await()
        return playlistRef.id
    }

    suspend fun renamePlaylist(userId: String, playlistId: String, name: String) {
        if (userId.isBlank() || playlistId.isBlank()) return
        val normalizedName = name.trim()
        if (normalizedName.isBlank()) return

        db.collection("users").document(userId)
            .collection(PLAYLISTS_COLLECTION)
            .document(playlistId)
            .set(
                mapOf(
                    "name" to normalizedName,
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            )
            .await()
    }

    suspend fun deletePlaylist(userId: String, playlistId: String) {
        if (userId.isBlank() || playlistId.isBlank()) return
        val playlistRef = db.collection("users").document(userId)
            .collection(PLAYLISTS_COLLECTION)
            .document(playlistId)
        val itemRefs = playlistRef.collection(PLAYLIST_ITEMS_COLLECTION)
            .get()
            .await()
            .documents
            .map { it.reference }
        val refsToDelete = itemRefs + playlistRef

        refsToDelete.chunked(FIRESTORE_BATCH_LIMIT).forEach { refs ->
            val batch = db.batch()
            refs.forEach(batch::delete)
            batch.commit().await()
        }
    }

    suspend fun addMovieToPlaylist(
        userId: String,
        playlistId: String,
        movie: PlaylistMovieItem
    ) {
        if (userId.isBlank() || playlistId.isBlank() || movie.movieId.isBlank()) return
        if (!playlistRef(userId, playlistId).get().await().exists()) return

        playlistItemsRef(userId, playlistId)
            .document(movie.movieId)
            .set(
                mapOf(
                    "movieId" to movie.movieId,
                    "movieName" to movie.movieName,
                    "posterUrl" to movie.posterUrl,
                    "addedAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            )
            .await()
        refreshPlaylistSummary(userId, playlistId)
    }

    suspend fun removeMovieFromPlaylist(
        userId: String,
        playlistId: String,
        movieSlug: String
    ) {
        if (userId.isBlank() || playlistId.isBlank() || movieSlug.isBlank()) return
        playlistItemsRef(userId, playlistId)
            .document(movieSlug)
            .delete()
            .await()
        refreshPlaylistSummary(userId, playlistId)
    }

    private fun playlistItemsRef(userId: String, playlistId: String) =
        playlistRef(userId, playlistId)
            .collection(PLAYLIST_ITEMS_COLLECTION)

    private fun playlistRef(userId: String, playlistId: String) =
        db.collection("users").document(userId)
            .collection(PLAYLISTS_COLLECTION).document(playlistId)

    private suspend fun refreshPlaylistSummary(userId: String, playlistId: String) {
        val playlistRef = playlistRef(userId, playlistId)
        if (!playlistRef.get().await().exists()) return

        val items = playlistItemsRef(userId, playlistId)
            .orderBy("addedAt", Query.Direction.ASCENDING)
            .get()
            .await()
            .documents
            .mapNotNull { it.toObject(PlaylistMovieItem::class.java) }
        val coverPosterUrl = items.firstOrNull()?.posterUrl.orEmpty()

        playlistRef.set(
            mapOf(
                "coverPosterUrl" to coverPosterUrl,
                "itemCount" to items.size.toLong(),
                "updatedAt" to FieldValue.serverTimestamp()
            ),
            SetOptions.merge()
        ).await()
    }

    suspend fun postComment(
        movieId: String,
        userId: String,
        userName: String,
        userAvatar: String,
        content: String,
        parentCommentId: String = "",
        replyToUserName: String = ""
    ) {
        if (movieId.isBlank() || userId.isBlank() || content.isBlank()) return
        
        val commentsRef = db.collection("movies").document(movieId)
            .collection("comments")
            
        val comment = Comment(
            userId = userId,
            userName = userName,
            userAvatar = userAvatar,
            content = content,
            parentCommentId = parentCommentId,
            replyToUserName = replyToUserName,
            likedBy = emptyList()
        )
        commentsRef.add(comment).await()
    }

    suspend fun toggleCommentLike(
        movieId: String,
        commentId: String,
        userId: String
    ) {
        if (movieId.isBlank() || commentId.isBlank() || userId.isBlank()) return

        val commentRef = db.collection("movies").document(movieId)
            .collection("comments").document(commentId)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(commentRef)
            if (!snapshot.exists()) {
                throw IllegalStateException("Comment does not exist")
            }
            val likedBy = snapshot.get("likedBy")
                .let { value -> value as? List<*> }
                ?.mapNotNull { it as? String }
                .orEmpty()

            val updatedLikedBy = if (userId in likedBy) {
                likedBy.filterNot { it == userId }
            } else {
                likedBy + userId
            }

            transaction.set(
                commentRef,
                mapOf(
                    "likedBy" to updatedLikedBy,
                    "likes" to updatedLikedBy.size.toLong(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            )
        }.await()
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
    suspend fun getCategoryMovies(categoryId: String, isKidsMode: Boolean, limit: Int = 20): List<com.example.alphacinema.data.model.FirestoreMovie> {
        // ... (unchanged previous logic)
        return try {
            val categorySnapshot = db.collection("home_categories").document(categoryId).get().await()
            if (!categorySnapshot.exists()) return emptyList()

            val category = categorySnapshot.toObject(com.example.alphacinema.data.model.HomeCategory::class.java)
            val slugs = category?.movieSlugs ?: emptyList()
            if (slugs.isEmpty()) return emptyList()

            val chunks = slugs.chunked(30)
            val resultMovies = mutableListOf<com.example.alphacinema.data.model.FirestoreMovie>()

            for (chunk in chunks) {
                var query: Query = db.collection("movies").whereIn(com.google.firebase.firestore.FieldPath.documentId(), chunk)
                if (isKidsMode) {
                    query = query.whereEqualTo("isKidsFriendly", true)
                }
                
                val snapshot = query.get().await()
                val moviesInChunk = snapshot.documents.mapNotNull { doc ->
                    try {
                        val m = doc.toObject(com.example.alphacinema.data.model.FirestoreMovie::class.java)
                        m?.apply { slug = doc.id }
                    } catch (e: Exception) {
                        null
                    }
                }
                resultMovies.addAll(moviesInChunk)
                if (resultMovies.size >= limit) break
            }

            val sortedResult = mutableListOf<com.example.alphacinema.data.model.FirestoreMovie>()
            for (slug in slugs) {
                val foundMovie = resultMovies.find { it.slug == slug }
                if (foundMovie != null) {
                    sortedResult.add(foundMovie)
                }
                if (sortedResult.size >= limit) break
            }

            sortedResult
        } catch (e: Exception) {
            android.util.Log.e("FirestoreRepository", "getCategoryMovies failed for categoryId=$categoryId", e)
            emptyList()
        }
    }

    // --- ADMIN CATEGORY MANAGEMENT ---
    
    suspend fun getAllHomeCategories(): List<com.example.alphacinema.data.model.HomeCategory> {
        return try {
            val snapshot = db.collection("home_categories").get().await()
            snapshot.documents.mapNotNull { it.toObject(com.example.alphacinema.data.model.HomeCategory::class.java) }
        } catch (e: Exception) {
            android.util.Log.e("FirestoreRepository", "getAllHomeCategories failed", e)
            emptyList()
        }
    }

    suspend fun saveHomeCategory(category: com.example.alphacinema.data.model.HomeCategory) {
        if (category.id.isBlank()) return
        db.collection("home_categories").document(category.id).set(category).await()
    }

    suspend fun deleteHomeCategory(categoryId: String) {
        if (categoryId.isBlank()) return
        db.collection("home_categories").document(categoryId).delete().await()
    }
}

private const val WATCH_HISTORY_COLLECTION = "watch_history"
private const val PLAYLISTS_COLLECTION = "playlists"
private const val PLAYLIST_ITEMS_COLLECTION = "items"
private const val FIRESTORE_BATCH_LIMIT = 450
private const val KIDS_WATCH_HISTORY_DOCUMENT_PREFIX = "__kids__"
private const val WATCH_HISTORY_MODE_DEFAULT = "default"
private const val WATCH_HISTORY_MODE_KIDS = "kids"
