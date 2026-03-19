package com.android.zubanx.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DictionaryResponseDto(
    @SerialName("word") val word: String,
    @SerialName("phonetic") val phonetic: String? = null,
    @SerialName("phonetics") val phonetics: List<PhoneticDto> = emptyList(),
    @SerialName("meanings") val meanings: List<MeaningDto> = emptyList()
)

@Serializable
data class PhoneticDto(
    @SerialName("text") val text: String? = null,
    @SerialName("audio") val audio: String? = null
)

@Serializable
data class MeaningDto(
    @SerialName("partOfSpeech") val partOfSpeech: String,
    @SerialName("definitions") val definitions: List<DefinitionDto> = emptyList()
)

@Serializable
data class DefinitionDto(
    @SerialName("definition") val definition: String,
    @SerialName("example") val example: String? = null,
    @SerialName("synonyms") val synonyms: List<String> = emptyList(),
    @SerialName("antonyms") val antonyms: List<String> = emptyList()
)
