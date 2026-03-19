package com.android.zubanx.data.repository

import com.android.zubanx.data.local.db.dao.TranslationDao
import com.android.zubanx.data.local.db.mapper.toDomain
import com.android.zubanx.data.local.db.mapper.toEntity
import com.android.zubanx.domain.model.Translation
import com.android.zubanx.domain.repository.TranslationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TranslationRepositoryImpl(private val dao: TranslationDao) : TranslationRepository {

    override fun getHistory(): Flow<List<Translation>> =
        dao.getHistory().map { it.map { entity -> entity.toDomain() } }

    override suspend fun saveTranslation(translation: Translation) {
        dao.insert(translation.toEntity())
    }

    override suspend fun deleteTranslation(id: Long) {
        dao.deleteById(id)
    }

    override suspend fun clearHistory() {
        dao.clearAll()
    }
}
