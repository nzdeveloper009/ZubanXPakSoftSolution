package com.android.zubanx.domain.model

data class OfflineLanguagePack(
    val id: Long = 0L,
    val languageCode: String,
    val languageName: String,
    val isDownloaded: Boolean = false,
    val downloadedAt: Long? = null
)
