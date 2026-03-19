package com.android.zubanx.data.repository

import com.android.zubanx.data.local.db.dao.OfflineLanguagePackDao
import com.android.zubanx.data.local.db.mapper.toDomain
import com.android.zubanx.data.local.db.mapper.toEntity
import com.android.zubanx.domain.model.OfflineLanguagePack
import com.android.zubanx.domain.repository.OfflineLanguagePackRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class OfflineLanguagePackRepositoryImpl(
    private val dao: OfflineLanguagePackDao
) : OfflineLanguagePackRepository {

    override fun getAll(): Flow<List<OfflineLanguagePack>> =
        dao.getAll().map { it.map { entity -> entity.toDomain() } }

    override suspend fun getByLanguageCode(code: String): OfflineLanguagePack? =
        dao.getByLanguageCode(code)?.toDomain()

    override suspend fun markDownloaded(code: String, downloadedAt: Long) {
        dao.updateDownloadStatus(code, isDownloaded = true, downloadedAt = downloadedAt)
    }

    override suspend fun markRemoved(code: String) {
        dao.updateDownloadStatus(code, isDownloaded = false, downloadedAt = null)
    }
}
