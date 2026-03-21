package com.android.zubanx.data.remote.api

import com.android.zubanx.core.network.NetworkResult
import com.android.zubanx.core.network.safeApiCall
import com.android.zubanx.data.remote.dto.AiExpertResponseDto
import com.android.zubanx.data.remote.dto.AnthropicMessage
import com.android.zubanx.data.remote.dto.AnthropicRequestDto
import com.android.zubanx.data.remote.dto.AnthropicResponseDto
import com.android.zubanx.data.remote.dto.GeminiContent
import com.android.zubanx.data.remote.dto.GeminiPart
import com.android.zubanx.data.remote.dto.GeminiRequestDto
import com.android.zubanx.data.remote.dto.GeminiResponseDto
import com.android.zubanx.data.remote.dto.OpenAiMessage
import com.android.zubanx.data.remote.dto.OpenAiRequestDto
import com.android.zubanx.data.remote.dto.OpenAiResponseDto
import com.android.zubanx.security.KeyDecryptionModule
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType

class AiExpertServiceImpl(
    private val client: HttpClient,
    private val keyDecryptionModule: KeyDecryptionModule
) : AiExpertService {

    override suspend fun ask(expert: String, prompt: String): NetworkResult<AiExpertResponseDto> {
        if (expert == "DEFAULT") {
            return NetworkResult.Error("AiExpertService: DEFAULT expert should use TranslateApiService instead")
        }
        return when (expert.uppercase()) {
            "GPT" -> askGpt(prompt)
            "GEMINI" -> askGemini(prompt)
            "CLAUDE" -> askClaude(prompt)
            else -> NetworkResult.Error("Unknown expert: $expert")
        }
    }

    private suspend fun askGpt(prompt: String): NetworkResult<AiExpertResponseDto> = safeApiCall {
        val key = keyDecryptionModule.getDecryptedKey("openai_key_enc")
        if (key.isBlank()) throw IllegalStateException("GPT API key not available")
        val request = OpenAiRequestDto(
            model = "gpt-4o-mini",
            messages = listOf(OpenAiMessage(role = "user", content = prompt))
        )
        val response: OpenAiResponseDto = client.post {
            url(GPT_URL)
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $key")
            setBody(request)
        }.body()
        AiExpertResponseDto(
            expert = "GPT",
            content = response.choices.firstOrNull()?.message?.content
                ?: throw IllegalStateException("GPT returned empty response"),
            tokensUsed = response.usage?.totalTokens
        )
    }

    private suspend fun askGemini(prompt: String): NetworkResult<AiExpertResponseDto> = safeApiCall {
        val key = keyDecryptionModule.getDecryptedKey("gemini_key_enc")
        if (key.isBlank()) throw IllegalStateException("Gemini API key not available")
        val request = GeminiRequestDto(
            contents = listOf(GeminiContent(parts = listOf(GeminiPart(text = prompt))))
        )
        val response: GeminiResponseDto = client.post {
            url("$GEMINI_URL?key=$key")
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
        AiExpertResponseDto(
            expert = "GEMINI",
            content = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: throw IllegalStateException("Gemini returned empty response")
        )
    }

    private suspend fun askClaude(prompt: String): NetworkResult<AiExpertResponseDto> = safeApiCall {
        val key = keyDecryptionModule.getDecryptedKey("claude_key_enc")
        if (key.isBlank()) throw IllegalStateException("Claude API key not available")
        val request = AnthropicRequestDto(
            model = "claude-haiku-4-5-20251001",
            maxTokens = 1024,
            messages = listOf(AnthropicMessage(role = "user", content = prompt))
        )
        val response: AnthropicResponseDto = client.post {
            url(CLAUDE_URL)
            contentType(ContentType.Application.Json)
            header("x-api-key", key)
            header("anthropic-version", "2023-06-01")
            setBody(request)
        }.body()
        AiExpertResponseDto(
            expert = "CLAUDE",
            content = response.content.firstOrNull { it.type == "text" }?.text
                ?: throw IllegalStateException("Claude returned empty response"),
            tokensUsed = response.usage?.let { it.inputTokens + it.outputTokens }
        )
    }

    companion object {
        private const val GPT_URL = "https://api.openai.com/v1/chat/completions"
        private const val GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent"
        private const val CLAUDE_URL = "https://api.anthropic.com/v1/messages"

        fun buildTranslationPrompt(
            text: String,
            sourceLang: String,
            targetLang: String,
            tone: String = "original"
        ): String {
            val toneInstruction = when (tone) {
                "casual"       -> "Use a friendly and approachable tone."
                "professional" -> "Use a neutral, direct, professional tone."
                "education"    -> "Provide clear and instructional content."
                "friendly"     -> "Adopt a warm and welcoming tone."
                "funny"        -> "Add humor or playfulness to the translated text."
                else           -> "Use natural and authentic language."
            }
            return "Translate the following text from $sourceLang to $targetLang. $toneInstruction " +
                   "Reply with only the translated text, no explanations.\n\n$text"
        }
    }
}
