package com.example.alphacinema.data.repository

import com.example.alphacinema.data.model.WatchPartyChatMessage
import com.example.alphacinema.data.model.WatchPartyMember
import com.example.alphacinema.data.model.WatchPartyRoom
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

    companion object {
        const val MAX_MEMBERS = 5
        private val ROOM_ID_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    }

    private fun generateRoomId(): String {
        return (1..6).map { ROOM_ID_CHARS.random() }.joinToString("")
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
        val roomId = generateRoomId()
        val now = System.currentTimeMillis()

        val roomData = mapOf(
            "roomId" to roomId,
            "hostId" to hostId,
            "hostName" to hostName,
            "movieSlug" to movieSlug,
            "movieTitle" to movieTitle,
            "moviePosterUrl" to moviePosterUrl,
            "episodeId" to (episodeId ?: ""),
            "episodeName" to (episodeName ?: ""),
            "playbackState" to "paused",
            "currentTimeSec" to 0.0,
            "lastUpdated" to ServerValue.TIMESTAMP,
            "createdAt" to now,
            "maxMembers" to MAX_MEMBERS
        )

        val roomRef = rootRef.child(roomId)
        roomRef.setValue(roomData).await()

        // Add host as first member
        val member = mapOf(
            "uid" to hostId,
            "displayName" to hostName,
            "photoUrl" to hostPhotoUrl
        )
        roomRef.child("members").child(hostId).setValue(member).await()

        return roomId
    }

    // ── Join Room ───────────────────────────────────────────────────

    suspend fun joinRoom(
        roomId: String,
        uid: String,
        displayName: String,
        photoUrl: String
    ): Result<WatchPartyRoom> {
        val roomRef = rootRef.child(roomId)
        val snapshot = roomRef.get().await()

        if (!snapshot.exists()) {
            return Result.failure(Exception("Phòng không tồn tại"))
        }

        // Check member count
        val membersSnapshot = snapshot.child("members")
        val maxMembers = snapshot.child("maxMembers").getValue(Int::class.java) ?: MAX_MEMBERS

        if (membersSnapshot.childrenCount >= maxMembers && !membersSnapshot.hasChild(uid)) {
            return Result.failure(Exception("Phòng đã đầy ($maxMembers/$maxMembers người)"))
        }

        // Add member
        val member = mapOf(
            "uid" to uid,
            "displayName" to displayName,
            "photoUrl" to photoUrl
        )
        roomRef.child("members").child(uid).setValue(member).await()

        val room = snapshotToRoom(snapshot)
        return Result.success(room)
    }

    // ── Leave Room ──────────────────────────────────────────────────

    suspend fun leaveRoom(roomId: String, uid: String) {
        val roomRef = rootRef.child(roomId)
        val snapshot = roomRef.get().await()

        if (!snapshot.exists()) return

        val hostId = snapshot.child("hostId").getValue(String::class.java)

        if (uid == hostId) {
            // Host left → delete entire room
            roomRef.removeValue().await()
        } else {
            // Guest left → remove member
            roomRef.child("members").child(uid).removeValue().await()
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
        val updates = mapOf<String, Any>(
            "playbackState" to state,
            "currentTimeSec" to timeSec,
            "lastUpdated" to ServerValue.TIMESTAMP
        )
        rootRef.child(roomId).updateChildren(updates).await()
    }

    // ── Check if Room Exists ────────────────────────────────────────

    suspend fun roomExists(roomId: String): Boolean {
        val snapshot = rootRef.child(roomId).get().await()
        return snapshot.exists()
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
            lastUpdated = snapshot.child("lastUpdated").getValue(Long::class.java) ?: 0,
            createdAt = snapshot.child("createdAt").getValue(Long::class.java) ?: 0,
            maxMembers = snapshot.child("maxMembers").getValue(Int::class.java) ?: MAX_MEMBERS
        )
    }
}
