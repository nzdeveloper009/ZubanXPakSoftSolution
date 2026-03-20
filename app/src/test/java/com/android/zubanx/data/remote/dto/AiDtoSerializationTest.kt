package com.android.zubanx.data.remote.dto

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AiDtoSerializationTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `OpenAiRequestDto serializes correctly`() {
        val req = OpenAiRequestDto(
            model = "gpt-4o-mini",
            messages = listOf(OpenAiMessage(role = "user", content = "Hello"))
        )
        val encoded = json.encodeToString(req)
        assertTrue(encoded.contains("gpt-4o-mini"))
        assertTrue(encoded.contains("Hello"))
    }

    @Test
    fun `OpenAiResponseDto deserializes content`() {
        val rawJson = """
            {
              "choices": [{"message": {"role": "assistant", "content": "Hola"}, "finish_reason": "stop"}],
              "usage": {"prompt_tokens": 10, "completion_tokens": 5, "total_tokens": 15}
            }
        """.trimIndent()
        val dto = json.decodeFromString<OpenAiResponseDto>(rawJson)
        assertEquals("Hola", dto.choices.first().message.content)
    }

    @Test
    fun `GeminiRequestDto serializes correctly`() {
        val req = GeminiRequestDto(
            contents = listOf(GeminiContent(parts = listOf(GeminiPart(text = "Translate: Hello"))))
        )
        val encoded = json.encodeToString(req)
        assertTrue(encoded.contains("Translate: Hello"))
    }

    @Test
    fun `GeminiResponseDto deserializes text`() {
        val rawJson = """
            {
              "candidates": [{
                "content": {"parts": [{"text": "Hola"}], "role": "model"},
                "finishReason": "STOP"
              }]
            }
        """.trimIndent()
        val dto = json.decodeFromString<GeminiResponseDto>(rawJson)
        assertEquals("Hola", dto.candidates.first().content.parts.first().text)
    }

    @Test
    fun `AnthropicRequestDto serializes correctly`() {
        val req = AnthropicRequestDto(
            model = "claude-haiku-4-5-20251001",
            maxTokens = 1024,
            messages = listOf(AnthropicMessage(role = "user", content = "Translate: Hello"))
        )
        val encoded = json.encodeToString(req)
        assertTrue(encoded.contains("claude-haiku-4-5-20251001"))
        assertTrue(encoded.contains("Translate: Hello"))
    }

    @Test
    fun `AnthropicResponseDto deserializes text`() {
        val rawJson = """
            {
              "content": [{"type": "text", "text": "Hola"}],
              "stop_reason": "end_turn",
              "usage": {"input_tokens": 10, "output_tokens": 5}
            }
        """.trimIndent()
        val dto = json.decodeFromString<AnthropicResponseDto>(rawJson)
        assertEquals("Hola", dto.content.first().text)
    }
}
