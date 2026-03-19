package com.android.zubanx.data.local.db.mapper

import com.android.zubanx.data.local.db.entity.TranslationEntity
import com.android.zubanx.domain.model.Translation

fun TranslationEntity.toDomain(): Translation = Translation(
    id = id, sourceText = sourceText, translatedText = translatedText,
    sourceLang = sourceLang, targetLang = targetLang, expert = expert, timestamp = timestamp
)

fun Translation.toEntity(): TranslationEntity = TranslationEntity(
    id = id, sourceText = sourceText, translatedText = translatedText,
    sourceLang = sourceLang, targetLang = targetLang, expert = expert, timestamp = timestamp
)
