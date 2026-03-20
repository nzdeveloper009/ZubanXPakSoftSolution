package com.android.zubanx.data.remote.api

import com.android.zubanx.core.network.NetworkResult
import com.android.zubanx.data.remote.dto.DictionaryResponseDto
import com.android.zubanx.data.remote.dto.MeaningDto
import com.android.zubanx.data.remote.dto.DefinitionDto
import io.mockk.coEvery
import io.mockk.mockk
import io.ktor.client.HttpClient
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DictionaryApiServiceImplTest {

    @Test
    fun `BASE_URL is correct`() {
        assertEquals(
            "https://api.dictionaryapi.dev/api/v2/entries",
            DictionaryApiServiceImpl.BASE_URL
        )
    }

    @Test
    fun `parseFirstEntry extracts first item from JSON array`() {
        val json = Json { ignoreUnknownKeys = true }
        val rawJson = """
            [{"word":"hello","phonetic":"/həˈloʊ/","meanings":[
              {"partOfSpeech":"exclamation","definitions":[
                {"definition":"used as greeting","example":"Hello there!"}
              ]}
            ]}]
        """.trimIndent()
        val result = DictionaryApiServiceImpl.parseFirstEntry(rawJson, json)
        assertNotNull(result)
        assertEquals("hello", result!!.word)
        assertEquals("/həˈloʊ/", result.phonetic)
        assertEquals(1, result.meanings.size)
    }

    @Test
    fun `parseFirstEntry returns null for empty array`() {
        val json = Json { ignoreUnknownKeys = true }
        val result = DictionaryApiServiceImpl.parseFirstEntry("[]", json)
        assertTrue(result == null)
    }
}
