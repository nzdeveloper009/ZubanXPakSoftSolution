package com.android.zubanx.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favourites")
data class FavouriteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val sourceText: String,
    val translatedText: String,
    val sourceLang: String,
    val targetLang: String,
    val timestamp: Long,
    val category: String = "translate"
)

object FavouriteCategory {
    const val TRANSLATE = "translate"
    const val DICTIONARY = "dictionary"
}
