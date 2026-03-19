package com.android.zubanx.domain.repository

import com.android.zubanx.domain.model.Translation
import kotlinx.coroutines.flow.Flow

interface TranslationRepository {
    fun getHistory(): Flow<List<Translation>>
    suspend fun saveTranslation(translation: Translation)
    suspend fun deleteTranslation(id: Long)
    suspend fun clearHistory()
}
