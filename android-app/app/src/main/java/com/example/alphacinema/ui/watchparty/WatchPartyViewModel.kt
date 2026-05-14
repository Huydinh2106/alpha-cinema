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
import android.content.Context
import com.example.alphacinema.data.local.UserProfileCache
import io.agora.rtc2.ChannelMediaOptions
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.RtcEngineConfig

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

    // --- Agora Voice Chat State ---
    private var rtcEngine: RtcEngine? = null
    private var localAgoraUid: Int = 0

    private val _isMicMuted = MutableStateFlow(false)
    val isMicMuted: StateFlow<Boolean> = _isMicMuted.asStateFlow()

    // Map of integer UID (Firebase UID hashCode) to volume level (0-255)
    private val _speakingUsers = MutableStateFlow<Map<Int, Int>>(emptyMap())
    val speakingUsers: StateFlow<Map<Int, Int>> = _speakingUsers.asStateFlow()

    // TODO: BẠN CẦN THAY THẾ APP ID CỦA BẠN TẠI ĐÂY
    private val AGORA_APP_ID = "8d5392fa3903473d9ce043ec36666424" 

    private val rtcEventHandler = object : IRtcEngineEventHandler() {
        override fun onAudioVolumeIndication(speakers: Array<out AudioVolumeInfo>?, totalVolume: Int) {
            speakers?.let {
                val newMap = mutableMapOf<Int, Int>()
                it.forEach { speaker ->
                    val uid = if (speaker.uid == 0) localAgoraUid else speaker.uid
                    if (speaker.volume > 5) { // Lọc bớt tạp âm nhỏ
                        newMap[uid] = speaker.volume
                    }
                }
                _speakingUsers.value = newMap
            }
        }
    }
    // -----------------------------

    private var observeJob: Job? = null

    val isHost: Boolean
        get() = _room.value?.hostId == auth.currentUser?.uid

    val currentUid: String?
        get() = auth.currentUser?.uid

    // ── Create Room ─────────────────────────────────────────────────

    fun createRoom(
        context: Context,
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
                    hostPhotoUrl = getAvatarUrl(context, user),
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

    fun joinRoom(context: Context, roomId: String, onSuccess: () -> Unit) {
        val user = auth.currentUser ?: return
        viewModelScope.launch {
            _isJoining.value = true
            _error.value = null
            try {
                val result = repository.joinRoom(
                    roomId = roomId.uppercase().trim(),
                    uid = user.uid,
                    displayName = user.displayName ?: "Người dùng",
                    photoUrl = getAvatarUrl(context, user)
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

    fun leaveRoom() {
        val roomId = _room.value?.roomId ?: return
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                repository.leaveRoom(roomId, uid)
            } catch (_: Exception) { }
            stopObserving()
            
            // Giải phóng Agora
            rtcEngine?.leaveChannel()
            RtcEngine.destroy()
            rtcEngine = null
            _speakingUsers.value = emptyMap()
            
            _room.value = null
            _members.value = emptyList()
            _chatMessages.value = emptyList()
        }
    }

    // ── Voice Chat Control ──────────────────────────────────────────

    fun initAgora(context: Context, roomId: String) {
        if (rtcEngine != null) return
        try {
            val config = RtcEngineConfig()
            config.mContext = context.applicationContext
            config.mAppId = AGORA_APP_ID
            config.mEventHandler = rtcEventHandler
            rtcEngine = RtcEngine.create(config)
            
            // Bật module Audio
            rtcEngine?.enableAudio()
            rtcEngine?.enableAudioVolumeIndication(200, 3, true)
            
            val uidStr = auth.currentUser?.uid ?: return
            localAgoraUid = uidStr.hashCode() and 0x7FFFFFFF

            val options = ChannelMediaOptions()
            options.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
            options.channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION
            options.publishMicrophoneTrack = true
            options.autoSubscribeAudio = true
            
            val result = rtcEngine?.joinChannel("", roomId, localAgoraUid, options)
            println("Agora joinChannel result: $result (0 means success)")
            
            // Bật mic mặc định
            _isMicMuted.value = false
            rtcEngine?.muteLocalAudioStream(false)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun toggleMic() {
        val muted = !_isMicMuted.value
        _isMicMuted.value = muted
        rtcEngine?.muteLocalAudioStream(muted)
    }

    fun adjustUserVolume(firebaseUid: String, volume: Int) {
        // volume range: 0 - 100
        rtcEngine?.adjustUserPlaybackSignalVolume(firebaseUid.hashCode(), volume)
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

    private fun getAvatarUrl(context: Context, user: com.google.firebase.auth.FirebaseUser): String {
        val cache = UserProfileCache(context)
        // Priority: cached avatar URL (from Firebase Storage) > Firebase Auth photoUrl
        val cachedUrl = cache.avatarUrl
        if (cachedUrl.isNotBlank() && !cachedUrl.startsWith("/")) return cachedUrl
        return user.photoUrl?.toString() ?: ""
    }
}
