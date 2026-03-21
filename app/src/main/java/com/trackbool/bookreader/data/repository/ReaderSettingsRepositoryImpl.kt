package com.trackbool.bookreader.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.trackbool.bookreader.domain.model.ReaderSettings
import com.trackbool.bookreader.domain.model.ReaderSettingsDefaults
import com.trackbool.bookreader.domain.repository.ReaderSettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "reader_settings")

@Singleton
class ReaderSettingsRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : ReaderSettingsRepository {

    private object PreferencesKeys {
        val FONT_SIZE = floatPreferencesKey("font_size")
    }

    override val settings: Flow<ReaderSettings> = context.dataStore.data
        .map { preferences ->
            ReaderSettings(
                fontSize = preferences[PreferencesKeys.FONT_SIZE] ?: ReaderSettingsDefaults.FONT_SIZE_DEFAULT
            )
        }

    override suspend fun updateFontSize(size: Float) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.FONT_SIZE] = size
        }
    }
}
