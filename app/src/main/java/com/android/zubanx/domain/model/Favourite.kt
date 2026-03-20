package com.android.zubanx.domain.model

import android.os.Parcelable
import com.android.zubanx.data.local.db.entity.FavouriteCategory
import kotlinx.parcelize.Parcelize

@Parcelize
data class Favourite(
    val id: Long = 0L,
    val sourceText: String,
    val translatedText: String,
    val sourceLang: String,
    val targetLang: String,
    val timestamp: Long,
    val category: String = FavouriteCategory.TRANSLATE
) : Parcelable
