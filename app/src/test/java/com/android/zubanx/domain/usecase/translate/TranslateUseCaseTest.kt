package com.android.zubanx.domain.usecase.translate

import com.android.zubanx.core.network.NetworkResult
import com.android.zubanx.data.remote.api.AiExpertService
import com.android.zubanx.data.remote.api.TranslateApiService
import com.android.zubanx.data.remote.dto.AiExpertResponseDto
import com.android.zubanx.data.remote.dto.TranslateResponseDto
import com.android.zubanx.domain.repository.TranslationRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TranslateUseCaseTest {

    private val translateApi = mockk<TranslateApiService>()
    private val aiExpert = mockk<AiExpertService>()
    private val repository = mockk<TranslationRepository>(relaxed = true)
    private val useCase = TranslateUseCase(translateApi, aiExpert, repository)

    @Test
    fun `invoke with DEFAULT expert calls TranslateApiService`() = runTest {
        coEvery { translateApi.translate("Hello", "en", "es") } returns NetworkResult.Success(
            TranslateResponseDto(translatedText = "Hola", sourceLang = "en", targetLang = "es")
        )
        val result = useCase("Hello", "en", "es", "DEFAULT", "original")
        assertTrue(result is NetworkResult.Success)
        assertEquals("Hola", (result as NetworkResult.Success).data.translatedText)
        coVerify(exactly = 1) { translateApi.translate("Hello", "en", "es") }
    }

    @Test
    fun `invoke with GPT expert calls AiExpertService`() = runTest {
        coEvery { aiExpert.ask("GPT", any()) } returns NetworkResult.Success(
            AiExpertResponseDto(expert = "GPT", content = "Hola")
        )
        val result = useCase("Hello", "en", "es", "GPT", "original")
        assertTrue(result is NetworkResult.Success)
        assertEquals("Hola", (result as NetworkResult.Success).data.translatedText)
        coVerify(exactly = 1) { aiExpert.ask("GPT", any()) }
    }

    @Test
    fun `invoke on Success saves translation to repository`() = runTest {
        coEvery { translateApi.translate(any(), any(), any()) } returns NetworkResult.Success(
            TranslateResponseDto(translatedText = "Hola", sourceLang = "en", targetLang = "es")
        )
        useCase("Hello", "en", "es", "DEFAULT", "original")
        coVerify(exactly = 1) { repository.saveTranslation(any()) }
    }

    @Test
    fun `invoke on Error does not save to repository`() = runTest {
        coEvery { translateApi.translate(any(), any(), any()) } returns NetworkResult.Error("Network error")
        val result = useCase("Hello", "en", "es", "DEFAULT", "original")
        assertTrue(result is NetworkResult.Error)
        coVerify(exactly = 0) { repository.saveTranslation(any()) }
    }

    @Test
    fun `invoke with blank text returns error without calling API`() = runTest {
        val result = useCase("  ", "en", "es", "DEFAULT", "original")
        assertTrue(result is NetworkResult.Error)
        coVerify(exactly = 0) { translateApi.translate(any(), any(), any()) }
        coVerify(exactly = 0) { aiExpert.ask(any(), any()) }
    }
}
