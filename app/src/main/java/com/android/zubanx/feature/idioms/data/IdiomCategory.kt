package com.android.zubanx.feature.idioms.data

import com.android.zubanx.R

sealed class IdiomCategory(
    val id: String,
    val displayName: String,
    val iconRes: Int
) {
    object Common     : IdiomCategory("common",     "Common English", R.drawable.ic_category_greeting)
    object Business   : IdiomCategory("business",   "Business",       R.drawable.ic_category_office)
    object Food       : IdiomCategory("food",        "Food & Cooking", R.drawable.ic_category_dining)
    object Animals    : IdiomCategory("animals",     "Animals",        R.drawable.ic_category_travel)
    object Time       : IdiomCategory("time",        "Time & Money",   R.drawable.ic_category_shopping)
    object Weather    : IdiomCategory("weather",     "Weather",        R.drawable.ic_category_trouble)
    object Emotions   : IdiomCategory("emotions",    "Emotions",       R.drawable.ic_category_entertainment)
    object Travel     : IdiomCategory("travel",      "Travel",         R.drawable.ic_category_hotel)

    companion object {
        val ALL: List<IdiomCategory> = listOf(
            Common, Business, Food, Animals, Time, Weather, Emotions, Travel
        )
        fun fromId(id: String): IdiomCategory = ALL.first { it.id == id }
    }
}
