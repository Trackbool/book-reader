package com.trackbool.bookreader.domain.repository

import com.trackbool.bookreader.domain.model.ReaderSettings
import kotlinx.coroutines.flow.Flow

interface ReaderSettingsRepository {
    val settings: Flow<ReaderSettings>
    suspend fun updateFontSize(size: Float)
}
