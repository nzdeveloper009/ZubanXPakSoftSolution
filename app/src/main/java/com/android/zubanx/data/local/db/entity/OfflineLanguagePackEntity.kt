package com.android.zubanx.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "offline_language_packs")
data class OfflineLanguagePackEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val languageCode: String,
    val languageName: String,
    val isDownloaded: Boolean = false,
    val downloadedAt: Long? = null
)
