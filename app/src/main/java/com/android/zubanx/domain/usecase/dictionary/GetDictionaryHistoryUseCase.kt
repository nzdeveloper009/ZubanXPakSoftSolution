package com.android.zubanx.domain.usecase.dictionary

import com.android.zubanx.domain.model.DictionaryEntry
import com.android.zubanx.domain.repository.DictionaryRepository
import kotlinx.coroutines.flow.Flow

class GetDictionaryHistoryUseCase(private val repository: DictionaryRepository) {
    operator fun invoke(): Flow<List<DictionaryEntry>> = repository.getAll()
}
