package com.android.zubanx.domain.usecase.translate

import com.android.zubanx.domain.model.Favourite
import com.android.zubanx.domain.model.Translation
import com.android.zubanx.domain.repository.FavouriteRepository

class AddFavouriteFromTranslationUseCase(private val favouriteRepository: FavouriteRepository) {
    suspend operator fun invoke(translation: Translation) {
        favouriteRepository.add(
            Favourite(
                sourceText = translation.sourceText,
                translatedText = translation.translatedText,
                sourceLang = translation.sourceLang,
                targetLang = translation.targetLang,
                timestamp = System.currentTimeMillis()
            )
        )
    }
}
