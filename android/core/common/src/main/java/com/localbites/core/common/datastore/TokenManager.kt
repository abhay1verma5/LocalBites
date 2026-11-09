package com.localbites.core.common.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "localbites_prefs")

@Singleton
class TokenManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val ACCESS_TOKEN = stringPreferencesKey("access_token")
        private val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        private val USER_ROLE = stringPreferencesKey("user_role")
        private val USER_PHONE = stringPreferencesKey("user_phone")
    }

    val accessToken: Flow<String?> = context.dataStore.data.map { it[ACCESS_TOKEN] }
    val refreshToken: Flow<String?> = context.dataStore.data.map { it[REFRESH_TOKEN] }
    val userRole: Flow<String?> = context.dataStore.data.map { it[USER_ROLE] }
    val userPhone: Flow<String?> = context.dataStore.data.map { it[USER_PHONE] }

    val isLoggedIn: Flow<Boolean> = context.dataStore.data.map { it[ACCESS_TOKEN] != null }

    suspend fun saveTokens(accessToken: String, refreshToken: String) {
        context.dataStore.edit { prefs ->
            prefs[ACCESS_TOKEN] = accessToken
            prefs[REFRESH_TOKEN] = refreshToken
        }
    }

    suspend fun saveUserRole(role: String) {
        context.dataStore.edit { it[USER_ROLE] = role }
    }

    suspend fun saveUserPhone(phone: String) {
        context.dataStore.edit { it[USER_PHONE] = phone }
    }

    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }
}
