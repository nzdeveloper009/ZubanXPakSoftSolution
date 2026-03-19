package com.android.zubanx.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dictionary")
data class DictionaryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val word: String,
    val language: String,
    val definition: String,
    val phonetic: String? = null,
    val partOfSpeech: String? = null,
    val examplesJson: String = "[]",
    val timestamp: Long
)
