package com.android.zubanx.domain.usecase.conversation

import com.android.zubanx.core.network.NetworkResult
import com.android.zubanx.data.remote.api.TranslateApiService
import com.android.zubanx.data.remote.dto.TranslateResponseDto
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationTranslateUseCaseTest {

    private val apiService: TranslateApiService = mockk()
    private val useCase = ConversationTranslateUseCase(apiService)

    @Test
    fun `returns success when api succeeds`() = runTest {
        coEvery { apiService.translate("hello", "en", "ur") } returns
            NetworkResult.Success(TranslateResponseDto("سلام", "en", "ur"))

        val result = useCase("hello", "en", "ur")

        assertTrue(result is NetworkResult.Success)
        assertEquals("سلام", (result as NetworkResult.Success).data.translatedText)
    }

    @Test
    fun `trims whitespace before calling api`() = runTest {
        coEvery { apiService.translate("hello", "en", "ur") } returns
            NetworkResult.Success(TranslateResponseDto("سلام", "en", "ur"))

        val result = useCase(" hello ", "en", "ur")

        assertTrue(result is NetworkResult.Success)
        assertEquals("سلام", (result as NetworkResult.Success).data.translatedText)
        coVerify(exactly = 1) { apiService.translate("hello", "en", "ur") }
    }

    @Test
    fun `returns error when text is blank`() = runTest {
        val result = useCase("  ", "en", "ur")
        assertTrue(result is NetworkResult.Error)
        assertEquals("Text must not be blank", (result as NetworkResult.Error).message)
        coVerify(exactly = 0) { apiService.translate(any(), any(), any()) }
    }

    @Test
    fun `returns api error when api fails`() = runTest {
        coEvery { apiService.translate(any(), any(), any()) } returns
            NetworkResult.Error("Network error")

        val result = useCase("hello", "en", "ur")
        assertTrue(result is NetworkResult.Error)
    }
}
