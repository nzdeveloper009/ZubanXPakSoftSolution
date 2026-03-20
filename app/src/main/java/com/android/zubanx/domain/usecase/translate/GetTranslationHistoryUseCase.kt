package com.android.zubanx.domain.usecase.translate

import com.android.zubanx.domain.model.Translation
import com.android.zubanx.domain.repository.TranslationRepository
import kotlinx.coroutines.flow.Flow

class GetTranslationHistoryUseCase(private val repository: TranslationRepository) {
    operator fun invoke(): Flow<List<Translation>> = repository.getHistory()
}
