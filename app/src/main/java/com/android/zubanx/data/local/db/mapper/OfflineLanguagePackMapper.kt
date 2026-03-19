package com.android.zubanx.data.local.db.mapper

import com.android.zubanx.data.local.db.entity.OfflineLanguagePackEntity
import com.android.zubanx.domain.model.OfflineLanguagePack

fun OfflineLanguagePackEntity.toDomain(): OfflineLanguagePack = OfflineLanguagePack(
    id = id, languageCode = languageCode, languageName = languageName,
    isDownloaded = isDownloaded, downloadedAt = downloadedAt
)

fun OfflineLanguagePack.toEntity(): OfflineLanguagePackEntity = OfflineLanguagePackEntity(
    id = id, languageCode = languageCode, languageName = languageName,
    isDownloaded = isDownloaded, downloadedAt = downloadedAt
)
