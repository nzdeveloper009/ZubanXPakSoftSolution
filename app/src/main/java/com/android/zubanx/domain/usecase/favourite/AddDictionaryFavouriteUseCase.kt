package com.android.zubanx.domain.usecase.favourite

import com.android.zubanx.domain.model.Favourite
import com.android.zubanx.domain.model.FavouriteCategory
import com.android.zubanx.domain.repository.FavouriteRepository

class AddDictionaryFavouriteUseCase(private val repository: FavouriteRepository) {
    suspend operator fun invoke(word: String, definition: String, language: String) {
        val favourite = Favourite(
            sourceText = word,
            translatedText = definition,
            sourceLang = language,
            targetLang = "",
            category = FavouriteCategory.DICTIONARY,
            timestamp = System.currentTimeMillis()
        )
        repository.add(favourite)
    }
}
