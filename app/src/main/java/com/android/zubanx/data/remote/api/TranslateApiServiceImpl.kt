package com.android.zubanx.data.remote.api

import com.android.zubanx.core.network.NetworkResult
import com.android.zubanx.core.network.safeApiCall
import com.android.zubanx.data.remote.dto.TranslateResponseDto
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders

class TranslateApiServiceImpl(
    private val client: HttpClient
) : TranslateApiService {

    override suspend fun translate(
        text: String,
        sourceLang: String,
        targetLang: String
    ): NetworkResult<TranslateResponseDto> = safeApiCall {
        val html = client.get(BASE_URL) {
            parameter("hl", "en")
            parameter("sl", if (sourceLang == "auto") "auto" else sourceLang)
            parameter("tl", targetLang.trim())
            parameter("q", text)
            header(HttpHeaders.UserAgent, MOBILE_USER_AGENT)
            header(HttpHeaders.AcceptLanguage, "en-US,en;q=0.9")
        }.bodyAsText()

        val translated = parseTranslatedText(html)
        if (translated.isEmpty()) {
            throw IllegalStateException("Translation parsing failed — empty result")
        }

        TranslateResponseDto(
            translatedText = translated,
            sourceLang = sourceLang,
            targetLang = targetLang,
            detectedSourceLang = if (sourceLang == "auto") extractDetectedLang(html) else null
        )
    }

    companion object {
        private const val BASE_URL = "https://translate.google.com/m"
        private const val MOBILE_USER_AGENT =
            "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"

        /** Extracts translation from Google Translate mobile HTML response. */
        fun parseTranslatedText(html: String): String {
            val regex = Regex("""<div class="result-container">(.*?)</div>""", RegexOption.DOT_MATCHES_ALL)
            val raw = regex.find(html)?.groupValues?.getOrNull(1) ?: return ""
            return decodeHtmlEntities(raw.trim())
        }

        private fun extractDetectedLang(html: String): String? {
            val regex = Regex("""sl=([a-z]{2,3})""")
            return regex.find(html)?.groupValues?.getOrNull(1)
        }

        fun decodeHtmlEntities(text: String): String = text
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&nbsp;", " ")
            .replace(Regex("&#(\\d+);")) { match ->
                match.groupValues[1].toIntOrNull()?.toChar()?.toString() ?: match.value
            }
    }
}
