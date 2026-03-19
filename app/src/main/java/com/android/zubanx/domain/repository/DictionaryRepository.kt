package com.android.zubanx.domain.repository

import com.android.zubanx.domain.model.DictionaryEntry
import kotlinx.coroutines.flow.Flow

interface DictionaryRepository {
    fun getAll(): Flow<List<DictionaryEntry>>
    suspend fun getCached(word: String, language: String): DictionaryEntry?
    suspend fun saveToCache(entry: DictionaryEntry)
}
