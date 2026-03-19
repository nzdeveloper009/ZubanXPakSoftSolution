package com.android.zubanx.data.local.db.mapper

import com.android.zubanx.data.local.db.entity.FavouriteEntity
import com.android.zubanx.domain.model.Favourite

fun FavouriteEntity.toDomain(): Favourite = Favourite(
    id = id, sourceText = sourceText, translatedText = translatedText,
    sourceLang = sourceLang, targetLang = targetLang, timestamp = timestamp
)

fun Favourite.toEntity(): FavouriteEntity = FavouriteEntity(
    id = id, sourceText = sourceText, translatedText = translatedText,
    sourceLang = sourceLang, targetLang = targetLang, timestamp = timestamp
)
