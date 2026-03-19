package com.android.zubanx.domain.repository

import com.android.zubanx.domain.model.OfflineLanguagePack
import kotlinx.coroutines.flow.Flow

interface OfflineLanguagePackRepository {
    fun getAll(): Flow<List<OfflineLanguagePack>>
    suspend fun getByLanguageCode(code: String): OfflineLanguagePack?
    suspend fun markDownloaded(code: String, downloadedAt: Long)
    suspend fun markRemoved(code: String)
}
