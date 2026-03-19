package com.android.zubanx.domain.model

data class Favourite(
    val id: Long = 0L,
    val sourceText: String,
    val translatedText: String,
    val sourceLang: String,
    val targetLang: String,
    val timestamp: Long
)
