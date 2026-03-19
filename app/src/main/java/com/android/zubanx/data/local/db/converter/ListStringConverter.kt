package com.android.zubanx.data.local.db.converter

import androidx.room.TypeConverter
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

class ListStringConverter {
    @TypeConverter
    fun fromList(list: List<String>): String =
        Json.encodeToString(ListSerializer(String.serializer()), list)

    @TypeConverter
    fun toList(json: String): List<String> =
        runCatching { Json.decodeFromString(ListSerializer(String.serializer()), json) }
            .getOrDefault(emptyList())
}
