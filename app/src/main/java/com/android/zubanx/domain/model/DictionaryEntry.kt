package com.android.zubanx.domain.model

data class DictionaryEntry(
    val id: Long = 0L,
    val word: String,
    val language: String,
    val definition: String,
    val phonetic: String? = null,
    val partOfSpeech: String? = null,
    val examples: List<String> = emptyList(),
    val timestamp: Long
)
