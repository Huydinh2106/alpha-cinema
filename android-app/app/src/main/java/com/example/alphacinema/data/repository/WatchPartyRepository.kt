package com.example.alphacinema.data.repository

import com.example.alphacinema.data.api.RetrofitClient
import com.example.alphacinema.data.model.CreateWatchPartyRoomRequest
import com.example.alphacinema.data.model.JoinWatchPartyRoomRequest
import com.example.alphacinema.data.model.LeaveWatchPartyRoomRequest
import com.example.alphacinema.data.model.WatchPartyChatMessage
import com.example.alphacinema.data.model.WatchPartyMember
import com.example.alphacinema.data.model.WatchPartyRoom
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class WatchPartyRepository {

    private val db = FirebaseDatabase.getInstance()
    private val rootRef = db.getReference("watchParty")
    private val auth = FirebaseAuth.getInstance()
    private val api = RetrofitClient.watchPartyApi

    companion object {
        const val MAX_MEMBERS = 10
    }

    // ── Create Room ─────────────────────────────────────────────────

    suspend fun createRoom(
        hostId: String,
        hostName: String,
        hostPhotoUrl: String,
        movieSlug: String,
        movieTitle: String,
        moviePosterUrl: String,
        episodeId: String?,
        episodeName: String?
    ): String {
        val idToken = requireIdToken()
        val response = api.createRoom(
            authorization = "Bearer $idToken",
            request = CreateWatchPartyRoomRequest(
                movieSlug = movieSlug,
                movieTitle = movieTitle,
                moviePosterUrl = moviePosterUrl,
                episodeId = episodeId,
                episodeName = episodeName
            )
        )

        if (!response.isSuccessful) {
            throw Exception(response.functionErrorMessage("Không thể tạo phòng"))
        }

        return response.body()?.roomId?.takeIf { it.isNotBlank() }
            ?: throw Exception("Không nhận được mã phòng")
    }

    // ── Join Room ───────────────────────────────────────────────────

    suspend fun joinRoom(
        roomId: String,
        uid: String,
        displayName: String,
        photoUrl: String
    ): Result<WatchPartyRoom> {
        return try {
            val normalizedRoomId = roomId.uppercase().trim()
            val idToken = requireIdToken()
            val response = api.joinRoom(
                authorization = "Bearer $idToken",
                request = JoinWatchPartyRoomRequest(roomId = normalizedRoomId)
            )

            if (!response.isSuccessful) {
                return Result.failure(Exception(response.functionErrorMessage("Không thể tham gia phòng")))
            }

            val snapshot = rootRef.child(normalizedRoomId).get().await()
            if (!snapshot.exists()) {
                Result.failure(Exception("Phòng không tồn tại"))
            } else {
                Result.success(snapshotToRoom(snapshot))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Leave Room ──────────────────────────────────────────────────

    suspend fun leaveRoom(roomId: String, uid: String) {
        if (roomId.isBlank()) return
        val idToken = requireIdToken()
        val response = api.leaveRoom(
            authorization = "Bearer $idToken",
            request = LeaveWatchPartyRoomRequest(roomId = roomId.uppercase().trim())
        )
        if (!response.isSuccessful) {
            throw Exception(response.functionErrorMessage("Không thể rời phòng"))
        }
    }

    // ── Observe Room State ──────────────────────────────────────────

    fun observeRoom(roomId: String): Flow<WatchPartyRoom?> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    trySend(null)
                    return
                }
                trySend(snapshotToRoom(snapshot))
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        val ref = rootRef.child(roomId)
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    // ── Observe Members ─────────────────────────────────────────────

    fun observeMembers(roomId: String): Flow<List<WatchPartyMember>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val members = snapshot.children.mapNotNull { child ->
                    WatchPartyMember(
                        uid = child.child("uid").getValue(String::class.java) ?: return@mapNotNull null,
                        displayName = child.child("displayName").getValue(String::class.java) ?: "",
                        photoUrl = child.child("photoUrl").getValue(String::class.java) ?: ""
                    )
                }
                trySend(members)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        val ref = rootRef.child(roomId).child("members")
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    // ── Observe Chat ────────────────────────────────────────────────

    fun observeChat(roomId: String): Flow<List<WatchPartyChatMessage>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val messages = snapshot.children.mapNotNull { child ->
                    WatchPartyChatMessage(
                        id = child.key ?: return@mapNotNull null,
                        uid = child.child("uid").getValue(String::class.java) ?: "",
                        displayName = child.child("displayName").getValue(String::class.java) ?: "",
                        text = child.child("text").getValue(String::class.java) ?: "",
                        timestamp = child.child("timestamp").getValue(Long::class.java) ?: 0
                    )
                }
                trySend(messages)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        val ref = rootRef.child(roomId).child("chat")
        ref.orderByChild("timestamp").addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    // ── Send Chat Message ───────────────────────────────────────────

    suspend fun sendChatMessage(
        roomId: String,
        uid: String,
        displayName: String,
        text: String
    ) {
        val chatRef = rootRef.child(roomId).child("chat").push()
        val message = mapOf(
            "uid" to uid,
            "displayName" to displayName,
            "text" to text,
            "timestamp" to ServerValue.TIMESTAMP
        )
        chatRef.setValue(message).await()
    }

    // ── Host: Update Playback State ────────────────────────────────

    suspend fun updatePlaybackState(
        roomId: String,
        state: String,
        timeSec: Double
    ) {
        val updates = mutableMapOf<String, Any>(
            "playbackState" to state,
            "currentTimeSec" to timeSec,
            "lastUpdated" to ServerValue.TIMESTAMP
        )
        if (state == "playing") {
            // Server Clock: record the server timestamp when play started
            updates["playStartedAt"] = ServerValue.TIMESTAMP
        }
        rootRef.child(roomId).updateChildren(updates).await()
    }

    // ── Check if Room Exists ────────────────────────────────────────

    suspend fun roomExists(roomId: String): Boolean {
        val snapshot = rootRef.child(roomId).get().await()
        return snapshot.exists()
    }

    // ── Host: Update Episode/Movie ──────────────────────────────────

    suspend fun updateEpisode(
        roomId: String,
        episodeId: String,
        episodeName: String
    ) {
        val updates = mapOf<String, Any>(
            "episodeId" to episodeId,
            "episodeName" to episodeName,
            "playbackState" to "paused",
            "currentTimeSec" to 0.0,
            "playStartedAt" to 0,
            "lastUpdated" to ServerValue.TIMESTAMP
        )
        rootRef.child(roomId).updateChildren(updates).await()
    }

    suspend fun updateMovie(
        roomId: String,
        movieSlug: String,
        movieTitle: String,
        moviePosterUrl: String,
        episodeId: String?,
        episodeName: String?
    ) {
        val updates = mapOf<String, Any>(
            "movieSlug" to movieSlug,
            "movieTitle" to movieTitle,
            "moviePosterUrl" to moviePosterUrl,
            "episodeId" to (episodeId ?: ""),
            "episodeName" to (episodeName ?: ""),
            "playbackState" to "paused",
            "currentTimeSec" to 0.0,
            "playStartedAt" to 0,
            "lastUpdated" to ServerValue.TIMESTAMP
        )
        rootRef.child(roomId).updateChildren(updates).await()
    }

    // ── Helper ──────────────────────────────────────────────────────

    private fun snapshotToRoom(snapshot: DataSnapshot): WatchPartyRoom {
        return WatchPartyRoom(
            roomId = snapshot.child("roomId").getValue(String::class.java) ?: "",
            hostId = snapshot.child("hostId").getValue(String::class.java) ?: "",
            hostName = snapshot.child("hostName").getValue(String::class.java) ?: "",
            movieSlug = snapshot.child("movieSlug").getValue(String::class.java) ?: "",
            movieTitle = snapshot.child("movieTitle").getValue(String::class.java) ?: "",
            moviePosterUrl = snapshot.child("moviePosterUrl").getValue(String::class.java) ?: "",
            episodeId = snapshot.child("episodeId").getValue(String::class.java)?.takeIf { it.isNotBlank() },
            episodeName = snapshot.child("episodeName").getValue(String::class.java)?.takeIf { it.isNotBlank() },
            playbackState = snapshot.child("playbackState").getValue(String::class.java) ?: "paused",
            currentTimeSec = snapshot.child("currentTimeSec").getValue(Double::class.java) ?: 0.0,
            playStartedAt = snapshot.child("playStartedAt").getValue(Long::class.java) ?: 0,
            lastUpdated = snapshot.child("lastUpdated").getValue(Long::class.java) ?: 0,
            createdAt = snapshot.child("createdAt").getValue(Long::class.java) ?: 0,
            maxMembers = snapshot.child("maxMembers").getValue(Int::class.java) ?: MAX_MEMBERS
        )
    }

    // ── Server Time Offset ──────────────────────────────────────────

    fun getServerTimeOffset(): Flow<Long> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val offset = snapshot.getValue(Long::class.java) ?: 0L
                trySend(offset)
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        val ref = db.getReference(".info/serverTimeOffset")
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    private suspend fun requireIdToken(): String {
        val user = auth.currentUser ?: throw Exception("Vui lòng đăng nhập để sử dụng xem chung")
        return user.getIdToken(false).await().token
            ?: throw Exception("Không lấy được phiên đăng nhập")
    }

    private fun retrofit2.Response<*>.functionErrorMessage(fallback: String): String {
        val bodyMessage = runCatching {
            errorBody()?.string()
                ?.substringAfter("\"error\":\"", "")
                ?.substringBefore("\"")
                ?.takeIf { it.isNotBlank() }
        }.getOrNull()
        return bodyMessage ?: message().takeIf { it.isNotBlank() } ?: fallback
    }
}
