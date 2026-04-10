package com.example.alphacinema.data.local

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsManager private constructor(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

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

    companion object {
        private const val PREFS_NAME = "alpha_cinema_settings"
        private const val KEY_KIDS_MODE = "kids_mode_enabled"
        private const val KEY_KIDS_MODE_PIN = "kids_mode_pin"

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
