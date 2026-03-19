package com.android.zubanx.data.local.db.mapper

import com.android.zubanx.data.local.db.entity.DictionaryEntity
import com.android.zubanx.domain.model.DictionaryEntry
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

fun DictionaryEntity.toDomain(): DictionaryEntry = DictionaryEntry(
    id = id, word = word, language = language, definition = definition,
    phonetic = phonetic, partOfSpeech = partOfSpeech,
    examples = runCatching {
        Json.decodeFromString(ListSerializer(String.serializer()), examplesJson)
    }.getOrDefault(emptyList()),
    timestamp = timestamp
)

fun DictionaryEntry.toEntity(): DictionaryEntity = DictionaryEntity(
    id = id, word = word, language = language, definition = definition,
    phonetic = phonetic, partOfSpeech = partOfSpeech,
    examplesJson = runCatching {
        Json.encodeToString(ListSerializer(String.serializer()), examples)
    }.getOrDefault("[]"),
    timestamp = timestamp
)
