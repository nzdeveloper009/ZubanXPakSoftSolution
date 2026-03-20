package com.android.zubanx.domain.usecase.phrases

import com.android.zubanx.core.network.NetworkResult
import com.android.zubanx.data.remote.api.TranslateApiService
import com.android.zubanx.data.remote.dto.TranslateResponseDto
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TranslatePhraseUseCaseTest {

    private val apiService: TranslateApiService = mockk()
    private val useCase = TranslatePhraseUseCase(apiService)

    @Test
    fun `returns success with translated text`() = runTest {
        coEvery { apiService.translate("Hello", "en", "ur") } returns
            NetworkResult.Success(TranslateResponseDto("ہیلو", "en", "ur"))

        val result = useCase("Hello", "en", "ur")

        assertTrue(result is NetworkResult.Success)
        assertEquals("ہیلو", (result as NetworkResult.Success).data.translatedText)
    }

    @Test
    fun `returns error when text is blank`() = runTest {
        val result = useCase("   ", "en", "ur")
        assertTrue(result is NetworkResult.Error)
        assertEquals("Text must not be blank", (result as NetworkResult.Error).message)
        coVerify(exactly = 0) { apiService.translate(any(), any(), any()) }
    }

    @Test
    fun `trims whitespace before calling api`() = runTest {
        coEvery { apiService.translate("Hello", "en", "ur") } returns
            NetworkResult.Success(TranslateResponseDto("ہیلو", "en", "ur"))

        useCase("  Hello  ", "en", "ur")

        coVerify(exactly = 1) { apiService.translate("Hello", "en", "ur") }
    }

    @Test
    fun `returns api error when api fails`() = runTest {
        coEvery { apiService.translate(any(), any(), any()) } returns
            NetworkResult.Error("Network error")

        val result = useCase("A table for two, please.", "en", "ur")
        assertTrue(result is NetworkResult.Error)
    }
}
