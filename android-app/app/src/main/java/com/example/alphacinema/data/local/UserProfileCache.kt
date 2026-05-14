package com.example.alphacinema.data.local

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File

/**
 * Local cache for user profile data.
 * Stores avatar image as a local file + URL/name in SharedPreferences.
 * This ensures the avatar loads instantly without network requests.
 */
class UserProfileCache(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("user_profile_cache", Context.MODE_PRIVATE)

    var avatarUrl: String
        get() = prefs.getString(KEY_AVATAR_URL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_AVATAR_URL, value).apply()

    var displayName: String
        get() = prefs.getString(KEY_DISPLAY_NAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_DISPLAY_NAME, value).apply()

    var uid: String
        get() = prefs.getString(KEY_UID, "") ?: ""
        set(value) = prefs.edit().putString(KEY_UID, value).apply()

    /** Path to the locally cached avatar image file */
    private val avatarFile: File
        get() = File(context.filesDir, "cached_avatar.jpg")

    /** Returns the local avatar file if it exists, otherwise null */
    val localAvatarFile: File?
        get() = avatarFile.takeIf { it.exists() && it.length() > 0 }

    fun save(uid: String, displayName: String, avatarUrl: String) {
        prefs.edit()
            .putString(KEY_UID, uid)
            .putString(KEY_DISPLAY_NAME, displayName)
            .putString(KEY_AVATAR_URL, avatarUrl)
            .apply()
    }

    /**
     * Save image bytes to local file for instant loading next time.
     */
    fun saveAvatarBytes(bytes: ByteArray) {
        try {
            avatarFile.outputStream().use { it.write(bytes) }
        } catch (_: Exception) {}
    }

    /**
     * Save avatar from a content URI (gallery/camera).
     */
    fun saveAvatarFromUri(context: Context, uri: android.net.Uri) {
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                avatarFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        } catch (_: Exception) {}
    }

    fun clear() {
        prefs.edit().clear().apply()
        try { avatarFile.delete() } catch (_: Exception) {}
    }

    companion object {
        private const val KEY_AVATAR_URL = "avatar_url"
        private const val KEY_DISPLAY_NAME = "display_name"
        private const val KEY_UID = "uid"
    }
}
