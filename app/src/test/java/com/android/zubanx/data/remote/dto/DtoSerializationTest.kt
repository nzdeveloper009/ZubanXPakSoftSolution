package com.android.zubanx.data.remote.dto

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DtoSerializationTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    @Test
    fun `TranslateResponseDto round-trips through JSON`() {
        val dto = TranslateResponseDto(
            translatedText = "Hola", sourceLang = "en",
            targetLang = "es", detectedSourceLang = null
        )
        val decoded = json.decodeFromString<TranslateResponseDto>(json.encodeToString(dto))
        assertEquals("Hola", decoded.translatedText)
        assertNull(decoded.detectedSourceLang)
    }

    @Test
    fun `TranslateResponseDto ignores unknown JSON keys`() {
        val raw = """{"translatedText":"Hi","sourceLang":"es","targetLang":"en","unknownField":"x"}"""
        assertEquals("Hi", json.decodeFromString<TranslateResponseDto>(raw).translatedText)
    }

    @Test
    fun `DictionaryResponseDto round-trips through JSON`() {
        val dto = DictionaryResponseDto(
            word = "run", phonetic = "/rʌn/",
            phonetics = listOf(PhoneticDto(text = "/rʌn/", audio = null)),
            meanings = listOf(MeaningDto(
                partOfSpeech = "verb",
                definitions = listOf(DefinitionDto(
                    definition = "to move fast", example = "She runs every morning.",
                    synonyms = listOf("sprint"), antonyms = emptyList()
                ))
            ))
        )
        val decoded = json.decodeFromString<DictionaryResponseDto>(json.encodeToString(dto))
        assertEquals("run", decoded.word)
        assertEquals("verb", decoded.meanings[0].partOfSpeech)
        assertEquals("to move fast", decoded.meanings[0].definitions[0].definition)
    }

    @Test
    fun `DictionaryResponseDto example and audio are nullable`() {
        val raw = """{"word":"cat","phonetics":[],"meanings":[{"partOfSpeech":"noun","definitions":[{"definition":"a small animal","synonyms":[],"antonyms":[]}]}]}"""
        val dto = json.decodeFromString<DictionaryResponseDto>(raw)
        assertEquals("cat", dto.word)
        assertNull(dto.phonetic)
        assertNull(dto.meanings[0].definitions[0].example)
    }

    @Test
    fun `AiExpertResponseDto round-trips through JSON`() {
        val dto = AiExpertResponseDto(
            expert = "GPT", content = "The word 'run' means to move swiftly.",
            tokensUsed = 42, errorMessage = null
        )
        val decoded = json.decodeFromString<AiExpertResponseDto>(json.encodeToString(dto))
        assertEquals("GPT", decoded.expert)
        assertEquals(42, decoded.tokensUsed)
        assertNull(decoded.errorMessage)
    }

    @Test
    fun `AiExpertResponseDto tokensUsed is nullable`() {
        val raw = """{"expert":"GEMINI","content":"hello"}"""
        val dto = json.decodeFromString<AiExpertResponseDto>(raw)
        assertEquals("GEMINI", dto.expert)
        assertNull(dto.tokensUsed)
    }

    @Test
    fun `AiExpertResponseDto models error response`() {
        val dto = AiExpertResponseDto(
            expert = "CLAUDE", content = "", tokensUsed = null,
            errorMessage = "rate limit exceeded"
        )
        assertEquals("rate limit exceeded", dto.errorMessage)
    }
}
