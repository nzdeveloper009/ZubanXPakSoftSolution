package com.android.zubanx.domain.model

data class Translation(
    val id: Long = 0L,
    val sourceText: String,
    val translatedText: String,
    val sourceLang: String,
    val targetLang: String,
    val expert: String,
    val timestamp: Long
)
