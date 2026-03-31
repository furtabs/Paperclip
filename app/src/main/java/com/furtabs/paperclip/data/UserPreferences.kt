package com.furtabs.paperclip.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class UserPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("kaeru_prefs", Context.MODE_PRIVATE)

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptedPrefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "kaeru_prefs_secure",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val _userName = MutableStateFlow(prefs.getString("name", "User") ?: "User")
    val userName = _userName.asStateFlow()
    private val _userBio = MutableStateFlow(prefs.getString("bio", "Bio") ?: "Bio")
    val userBio = _userBio.asStateFlow()
    private val _userAvatar = MutableStateFlow(prefs.getString("avatar_uri", null))
    val userAvatar = _userAvatar.asStateFlow()
    fun saveName(name: String) {
        prefs.edit { putString("name", name) }
        _userName.value = name
    }
    fun saveBio(bio: String) {
        prefs.edit { putString("bio", bio) }
        _userBio.value = bio
    }
    fun saveAvatar(uri: String) {
        prefs.edit { putString("avatar_uri", uri) }
        _userAvatar.value = uri
    }
    private val LANGUAGE_KEY = "app_language"
    fun saveLanguage(languageCode: String) {
        prefs.edit().putString(LANGUAGE_KEY, languageCode).apply()
    }
    fun getLanguage(): String {
        return prefs.getString(LANGUAGE_KEY, "system") ?: "system"
    }

    private val USPS_ACCESS_TOKEN = "usps_access_token"
    private val USPS_ACCESS_TOKEN_EXPIRY = "usps_access_token_expiry"

    fun saveUspsAccessToken(token: String, expiryMillis: Long) {
        encryptedPrefs.edit {
            putString(USPS_ACCESS_TOKEN, token)
            putLong(USPS_ACCESS_TOKEN_EXPIRY, expiryMillis)
        }
    }

    fun getUspsAccessToken(): String {
        return encryptedPrefs.getString(USPS_ACCESS_TOKEN, "") ?: ""
    }

    fun getUspsAccessTokenExpiry(): Long {
        return encryptedPrefs.getLong(USPS_ACCESS_TOKEN_EXPIRY, 0L)
    }
}
