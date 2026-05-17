package com.example.myandroidapp.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppPreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {

    val lastOpenedArticleId: Flow<Int?> = dataStore.data.map { prefs ->
        prefs[LAST_OPENED_ARTICLE_ID]
    }

    suspend fun setLastOpenedArticleId(id: Int) {
        dataStore.edit { prefs ->
            prefs[LAST_OPENED_ARTICLE_ID] = id
        }
    }

    val isDarkMode: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[IS_DARK_MODE] ?: false
    }

    suspend fun setDarkMode(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[IS_DARK_MODE] = enabled
        }
    }

    private companion object {
        private val LAST_OPENED_ARTICLE_ID = intPreferencesKey("last_opened_article_id")
        private val IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")
    }
}
