package com.example.alphacinema.ui.watchparty

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alphacinema.data.model.WatchPartyChatMessage
import com.example.alphacinema.data.model.WatchPartyMember
import com.example.alphacinema.data.model.WatchPartyRoom
import com.example.alphacinema.data.repository.WatchPartyRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WatchPartyViewModel : ViewModel() {

    private val repository = WatchPartyRepository()
    private val auth = FirebaseAuth.getInstance()

    private val _room = MutableStateFlow<WatchPartyRoom?>(null)
    val room: StateFlow<WatchPartyRoom?> = _room.asStateFlow()

    private val _members = MutableStateFlow<List<WatchPartyMember>>(emptyList())
    val members: StateFlow<List<WatchPartyMember>> = _members.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<WatchPartyChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<WatchPartyChatMessage>> = _chatMessages.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isCreating = MutableStateFlow(false)
    val isCreating: StateFlow<Boolean> = _isCreating.asStateFlow()

    private val _isJoining = MutableStateFlow(false)
    val isJoining: StateFlow<Boolean> = _isJoining.asStateFlow()

    private val _roomDismissed = MutableStateFlow(false)
    val roomDismissed: StateFlow<Boolean> = _roomDismissed.asStateFlow()

    private val _serverTimeOffset = MutableStateFlow(0L)
    val serverTimeOffset: StateFlow<Long> = _serverTimeOffset.asStateFlow()

    private var observeJob: Job? = null

    val isHost: Boolean
        get() = _room.value?.hostId == auth.currentUser?.uid

    val currentUid: String?
        get() = auth.currentUser?.uid

    // ── Create Room ─────────────────────────────────────────────────

    fun createRoom(
        movieSlug: String,
        movieTitle: String,
        moviePosterUrl: String,
        episodeId: String?,
        episodeName: String?,
        onSuccess: (String) -> Unit
    ) {
        val user = auth.currentUser ?: return
        viewModelScope.launch {
            _isCreating.value = true
            _error.value = null
            try {
                val roomId = repository.createRoom(
                    hostId = user.uid,
                    hostName = user.displayName ?: "Người dùng",
                    hostPhotoUrl = user.photoUrl?.toString() ?: "",
                    movieSlug = movieSlug,
                    movieTitle = movieTitle,
                    moviePosterUrl = moviePosterUrl,
                    episodeId = episodeId,
                    episodeName = episodeName
                )
                startObserving(roomId)
                onSuccess(roomId)
            } catch (e: Exception) {
                _error.value = e.message ?: "Không thể tạo phòng"
            } finally {
                _isCreating.value = false
            }
        }
    }

    // ── Join Room ───────────────────────────────────────────────────

    fun joinRoom(roomId: String, onSuccess: () -> Unit) {
        val user = auth.currentUser ?: return
        viewModelScope.launch {
            _isJoining.value = true
            _error.value = null
            try {
                val result = repository.joinRoom(
                    roomId = roomId.uppercase().trim(),
                    uid = user.uid,
                    displayName = user.displayName ?: "Người dùng",
                    photoUrl = user.photoUrl?.toString() ?: ""
                )
                result.fold(
                    onSuccess = {
                        startObserving(roomId.uppercase().trim())
                        onSuccess()
                    },
                    onFailure = { e ->
                        _error.value = e.message
                    }
                )
            } catch (e: Exception) {
                _error.value = e.message ?: "Không thể tham gia phòng"
            } finally {
                _isJoining.value = false
            }
        }
    }

    // ── Leave Room ──────────────────────────────────────────────────

    fun leaveRoom() {
        val roomId = _room.value?.roomId ?: return
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                repository.leaveRoom(roomId, uid)
            } catch (_: Exception) { }
            stopObserving()
            _room.value = null
            _members.value = emptyList()
            _chatMessages.value = emptyList()
        }
    }

    // ── Playback Control (Host only) ────────────────────────────────

    fun updatePlayback(state: String, timeSec: Double) {
        if (!isHost) return
        val roomId = _room.value?.roomId ?: return
        viewModelScope.launch {
            try {
                repository.updatePlaybackState(roomId, state, timeSec)
            } catch (_: Exception) { }
        }
    }

    // ── Change Episode (Host only) ──────────────────────────────────

    fun changeEpisode(episodeId: String, episodeName: String) {
        if (!isHost) return
        val roomId = _room.value?.roomId ?: return
        viewModelScope.launch {
            try {
                repository.updateEpisode(roomId, episodeId, episodeName)
            } catch (_: Exception) { }
        }
    }

    fun changeMovie(
        movieSlug: String,
        movieTitle: String,
        moviePosterUrl: String,
        episodeId: String?,
        episodeName: String?
    ) {
        if (!isHost) return
        val roomId = _room.value?.roomId ?: return
        viewModelScope.launch {
            try {
                repository.updateMovie(
                    roomId = roomId,
                    movieSlug = movieSlug,
                    movieTitle = movieTitle,
                    moviePosterUrl = moviePosterUrl,
                    episodeId = episodeId,
                    episodeName = episodeName
                )
            } catch (_: Exception) { }
        }
    }

    // ── Chat ────────────────────────────────────────────────────────

    fun sendMessage(text: String) {
        val roomId = _room.value?.roomId ?: return
        val user = auth.currentUser ?: return
        val msg = text.trim()
        if (msg.isBlank()) return

        viewModelScope.launch {
            try {
                repository.sendChatMessage(
                    roomId = roomId,
                    uid = user.uid,
                    displayName = user.displayName ?: "Người dùng",
                    text = msg
                )
            } catch (_: Exception) { }
        }
    }

    // ── Observe ─────────────────────────────────────────────────────

    private fun startObserving(roomId: String) {
        stopObserving()
        _roomDismissed.value = false
        observeJob = viewModelScope.launch {
            launch {
                repository.observeRoom(roomId).collect { room ->
                    if (room == null) {
                        // Room was deleted (host left)
                        _roomDismissed.value = true
                        _room.value = null
                    } else {
                        _room.value = room
                    }
                }
            }
            launch {
                repository.observeMembers(roomId).collect { members ->
                    _members.value = members
                }
            }
            launch {
                repository.observeChat(roomId).collect { messages ->
                    _chatMessages.value = messages
                }
            }
            launch {
                repository.getServerTimeOffset().collect { offset ->
                    _serverTimeOffset.value = offset
                }
            }
        }
    }

    private fun stopObserving() {
        observeJob?.cancel()
        observeJob = null
    }

    fun clearError() {
        _error.value = null
    }

    override fun onCleared() {
        super.onCleared()
        leaveRoom()
    }
}
