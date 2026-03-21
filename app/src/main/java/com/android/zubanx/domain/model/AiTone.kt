package com.android.zubanx.domain.model

enum class AiTone(val key: String, val label: String, val description: String) {
    ORIGINAL("original", "Original", "Natural and authentic language"),
    CASUAL("casual", "Casual", "Friendly and approachable tone"),
    PROFESSIONAL("professional", "Professional", "Neutral, direct, unaltered"),
    EDUCATIONAL("education", "Educational", "Clear and instructional content"),
    FRIENDLY("friendly", "Friendly", "Warm and welcoming tone"),
    FUNNY("funny", "Funny", "Add humor or playfulness to the text");

    companion object {
        fun fromKey(key: String) = entries.firstOrNull { it.key == key } ?: ORIGINAL
    }
}
