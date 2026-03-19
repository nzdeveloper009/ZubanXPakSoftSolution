package com.android.zubanx.data.local.db.mapper

import com.android.zubanx.data.local.db.entity.DictionaryEntity
import com.android.zubanx.domain.model.DictionaryEntry

fun DictionaryEntity.toDomain(): DictionaryEntry = DictionaryEntry(
    id = id, word = word, language = language, definition = definition,
    phonetic = phonetic, partOfSpeech = partOfSpeech,
    examples = examples,
    timestamp = timestamp
)

fun DictionaryEntry.toEntity(): DictionaryEntity = DictionaryEntity(
    id = id, word = word, language = language, definition = definition,
    phonetic = phonetic, partOfSpeech = partOfSpeech,
    examples = examples,
    timestamp = timestamp
)
