package com.android.zubanx.data.repository

import com.android.zubanx.data.local.db.dao.DictionaryDao
import com.android.zubanx.data.local.db.mapper.toDomain
import com.android.zubanx.data.local.db.mapper.toEntity
import com.android.zubanx.domain.model.DictionaryEntry
import com.android.zubanx.domain.repository.DictionaryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DictionaryRepositoryImpl(private val dao: DictionaryDao) : DictionaryRepository {

    override fun getAll(): Flow<List<DictionaryEntry>> =
        dao.getAll().map { it.map { entity -> entity.toDomain() } }

    override suspend fun getCached(word: String, language: String): DictionaryEntry? =
        dao.getByWordAndLanguage(word, language)?.toDomain()

    override suspend fun saveToCache(entry: DictionaryEntry) {
        dao.insert(entry.toEntity())
    }
}
