package com.android.zubanx.data.remote.api

import com.android.zubanx.core.network.NetworkResult
import com.android.zubanx.security.KeyDecryptionModule
import io.ktor.client.HttpClient
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class AiExpertServiceImplTest {

    private val client: HttpClient = mockk(relaxed = true)
    private val keyModule: KeyDecryptionModule = mockk {
        every { getDecryptedKey(any()) } returns ""
    }
    private val service = AiExpertServiceImpl(client, keyModule)

    @Test
    fun `ask with DEFAULT expert returns Error explaining to use TranslateApiService`() = runTest {
        val result = service.ask("DEFAULT", "Translate: Hello")
        assertTrue(result is NetworkResult.Error)
        assertTrue((result as NetworkResult.Error).message.contains("DEFAULT"))
    }

    @Test
    fun `ask with unknown expert returns Error`() = runTest {
        val result = service.ask("UNKNOWN_EXPERT", "some prompt")
        assertTrue(result is NetworkResult.Error)
    }

    @Test
    fun `buildTranslationPrompt contains source and target language and text`() {
        val prompt = AiExpertServiceImpl.buildTranslationPrompt("Hello", "en", "es")
        assertTrue(prompt.contains("Hello"))
        assertTrue(prompt.contains("en"))
        assertTrue(prompt.contains("es"))
        assertTrue(prompt.contains("only the translated"))
    }

    @Test
    fun `ask with GPT and empty key returns NetworkResult Error`() = runTest {
        every { keyModule.getDecryptedKey("openai_key_enc") } returns ""
        val result = service.ask("GPT", "Translate: Hello")
        assertTrue(result is NetworkResult.Error)
    }
}
