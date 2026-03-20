package com.android.zubanx.domain.usecase.translate

import com.android.zubanx.domain.repository.TranslationRepository

class DeleteTranslationUseCase(private val repository: TranslationRepository) {
    suspend operator fun invoke(id: Long) = repository.deleteTranslation(id)
}
