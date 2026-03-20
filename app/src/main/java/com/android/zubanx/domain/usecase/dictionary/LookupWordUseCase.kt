package com.android.zubanx.domain.usecase.dictionary

import com.android.zubanx.core.network.NetworkResult
import com.android.zubanx.data.remote.api.DictionaryApiService
import com.android.zubanx.domain.model.DictionaryEntry
import com.android.zubanx.domain.repository.DictionaryRepository

class LookupWordUseCase(
    private val apiService: DictionaryApiService,
    private val repository: DictionaryRepository
) {
    suspend operator fun invoke(word: String, language: String): NetworkResult<DictionaryEntry> {
        if (word.isBlank()) return NetworkResult.Error("Word must not be blank")

        val cached = repository.getCached(word.trim().lowercase(), language)
        if (cached != null) return NetworkResult.Success(cached)

        return when (val result = apiService.lookup(word.trim().lowercase(), language)) {
            is NetworkResult.Success -> {
                val dto = result.data
                val definition = dto.meanings.joinToString("\n\n") { meaning ->
                    val defs = meaning.definitions.mapIndexed { i, d ->
                        "${i + 1}. ${d.definition}"
                    }.joinToString("\n")
                    "[${meaning.partOfSpeech}]\n$defs"
                }.ifBlank { "No definition available" }

                val examples = dto.meanings.flatMap { meaning ->
                    meaning.definitions.mapNotNull { it.example }
                }

                val entry = DictionaryEntry(
                    word = dto.word,
                    language = language,
                    definition = definition,
                    phonetic = dto.phonetic ?: dto.phonetics.firstOrNull { !it.text.isNullOrBlank() }?.text,
                    partOfSpeech = dto.meanings.firstOrNull()?.partOfSpeech,
                    examples = examples,
                    timestamp = System.currentTimeMillis()
                )
                repository.saveToCache(entry)
                NetworkResult.Success(entry)
            }
            is NetworkResult.Error -> result
        }
    }
}
