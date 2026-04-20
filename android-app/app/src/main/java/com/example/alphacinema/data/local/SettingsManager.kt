package com.example.alphacinema.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.alphacinema.data.model.SupportChatMessage
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsManager private constructor(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _isKidsModeEnabled = MutableStateFlow(prefs.getBoolean(KEY_KIDS_MODE, false))
    val isKidsModeEnabled: StateFlow<Boolean> = _isKidsModeEnabled.asStateFlow()

    fun setKidsMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_KIDS_MODE, enabled).apply()
        _isKidsModeEnabled.value = enabled
    }

    fun getKidsModePin(): String? {
        return prefs.getString(KEY_KIDS_MODE_PIN, null)
    }

    fun setKidsModePin(pin: String?) {
        prefs.edit().putString(KEY_KIDS_MODE_PIN, pin).apply()
    }

    fun getSupportChatSessionId(): String? {
        return prefs.getString(KEY_SUPPORT_CHAT_SESSION_ID, null)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
    }

    fun getSupportChatMessages(): List<SupportChatMessage> {
        val json = prefs.getString(KEY_SUPPORT_CHAT_MESSAGES, null).orEmpty()
        if (json.isBlank()) return emptyList()

        return runCatching {
            gson.fromJson<List<SupportChatMessage>>(json, supportChatMessageListType)
        }.getOrDefault(emptyList())
    }

    fun saveSupportChatConversation(sessionId: String, messages: List<SupportChatMessage>) {
        prefs.edit().apply {
            putString(KEY_SUPPORT_CHAT_SESSION_ID, sessionId)
            putString(KEY_SUPPORT_CHAT_MESSAGES, gson.toJson(messages))
        }.apply()
    }

    fun clearSupportChatConversation() {
        prefs.edit()
            .remove(KEY_SUPPORT_CHAT_SESSION_ID)
            .remove(KEY_SUPPORT_CHAT_MESSAGES)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "alpha_cinema_settings"
        private const val KEY_KIDS_MODE = "kids_mode_enabled"
        private const val KEY_KIDS_MODE_PIN = "kids_mode_pin"
        private const val KEY_SUPPORT_CHAT_SESSION_ID = "support_chat_session_id"
        private const val KEY_SUPPORT_CHAT_MESSAGES = "support_chat_messages"
        private val supportChatMessageListType = object : TypeToken<List<SupportChatMessage>>() {}.type

        @Volatile
        private var INSTANCE: SettingsManager? = null

        fun init(context: Context) {
            if (INSTANCE == null) {
                synchronized(this) {
                    if (INSTANCE == null) {
                        INSTANCE = SettingsManager(context.applicationContext)
                    }
                }
            }
        }

        fun getInstance(): SettingsManager {
            return INSTANCE ?: throw IllegalStateException("SettingsManager not initialized")
        }
    }
}
