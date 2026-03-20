package com.android.zubanx.data.remote.api

import org.junit.Assert.assertEquals
import org.junit.Test

class TranslateApiServiceImplTest {

    @Test
    fun `parseTranslatedText extracts text from result-container div`() {
        val html = """
            <html><body>
            <div class="result-container">Hola mundo</div>
            </body></html>
        """.trimIndent()
        val result = TranslateApiServiceImpl.parseTranslatedText(html)
        assertEquals("Hola mundo", result)
    }

    @Test
    fun `parseTranslatedText returns empty string when div not found`() {
        val html = "<html><body><p>No translation here</p></body></html>"
        val result = TranslateApiServiceImpl.parseTranslatedText(html)
        assertEquals("", result)
    }

    @Test
    fun `parseTranslatedText decodes HTML entities`() {
        val html = """<div class="result-container">It&#39;s a &amp; test</div>"""
        val result = TranslateApiServiceImpl.parseTranslatedText(html)
        assertEquals("It's a & test", result)
    }

    @Test
    fun `parseTranslatedText handles Urdu text`() {
        val html = """<div class="result-container">ہیلو دنیا</div>"""
        val result = TranslateApiServiceImpl.parseTranslatedText(html)
        assertEquals("ہیلو دنیا", result)
    }

    @Test
    fun `parseTranslatedText handles Hindi text`() {
        val html = """<div class="result-container">नमस्ते दुनिया</div>"""
        val result = TranslateApiServiceImpl.parseTranslatedText(html)
        assertEquals("नमस्ते दुनिया", result)
    }

    @Test
    fun `decodeHtmlEntities handles numeric entities`() {
        val html = """<div class="result-container">&#65;&#66;&#67;</div>"""
        val result = TranslateApiServiceImpl.parseTranslatedText(html)
        assertEquals("ABC", result)
    }
}
