package com.android.zubanx.feature.idioms.data

data class IdiomEntry(
    val title: String,   // the idiom phrase, e.g. "Break a leg"
    val meaning: String, // plain explanation, e.g. "Good luck"
    val example: String  // usage sentence (always in English, not translated)
)
