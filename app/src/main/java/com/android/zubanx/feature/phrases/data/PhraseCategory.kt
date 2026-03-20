package com.android.zubanx.feature.phrases.data

import com.android.zubanx.R

sealed class PhraseCategory(
    val id: String,
    val displayName: String,
    val iconRes: Int
) {
    object Dining        : PhraseCategory("dining",        "Dining",        R.drawable.ic_category_dining)
    object Emergency     : PhraseCategory("emergency",     "Emergency",     R.drawable.ic_category_emergency)
    object Travel        : PhraseCategory("travel",        "Travel",        R.drawable.ic_category_travel)
    object Greeting      : PhraseCategory("greeting",      "Greeting",      R.drawable.ic_category_greeting)
    object Shopping      : PhraseCategory("shopping",      "Shopping",      R.drawable.ic_category_shopping)
    object Hotel         : PhraseCategory("hotel",         "Hotel",         R.drawable.ic_category_hotel)
    object Office        : PhraseCategory("office",        "Office",        R.drawable.ic_category_office)
    object Trouble       : PhraseCategory("trouble",       "Trouble",       R.drawable.ic_category_trouble)
    object Entertainment : PhraseCategory("entertainment", "Entertainment", R.drawable.ic_category_entertainment)
    object Medicine      : PhraseCategory("medicine",      "Medicine",      R.drawable.ic_category_medicine)
}
