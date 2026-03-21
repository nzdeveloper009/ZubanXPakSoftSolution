package com.android.zubanx.feature.story.data

import com.android.zubanx.R

sealed class StoryCategory(
    val id: String,
    val displayName: String,
    val iconRes: Int
) {
    object Adventure   : StoryCategory("adventure",   "Adventure",   R.drawable.ic_category_travel)
    object History     : StoryCategory("history",     "History",     R.drawable.ic_category_office)
    object Science     : StoryCategory("science",     "Science",     R.drawable.ic_category_medicine)
    object Culture     : StoryCategory("culture",     "Culture",     R.drawable.ic_category_entertainment)
    object Humor       : StoryCategory("humor",       "Humor",       R.drawable.ic_category_dining)
    object Motivation  : StoryCategory("motivation",  "Motivation",  R.drawable.ic_category_greeting)

    companion object {
        val ALL: List<StoryCategory> = listOf(Adventure, History, Science, Culture, Humor, Motivation)
        fun fromId(id: String): StoryCategory = ALL.first { it.id == id }
    }
}
