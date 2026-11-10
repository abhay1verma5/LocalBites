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

private val Context.languageDataStore by preferencesDataStore(name = "language_prefs")

@Singleton
class LanguageManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val SELECTED_LANGUAGE = stringPreferencesKey("selected_language")
        const val ENGLISH = "en"
        const val HINDI = "hi"
    }

    val selectedLanguage: Flow<String> = context.languageDataStore.data.map { prefs ->
        prefs[SELECTED_LANGUAGE] ?: ENGLISH
    }

    suspend fun setLanguage(languageCode: String) {
        context.languageDataStore.edit { prefs ->
            prefs[SELECTED_LANGUAGE] = languageCode
        }
    }
}
