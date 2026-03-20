package com.android.zubanx.domain.usecase.dictionary

import com.android.zubanx.core.network.NetworkResult
import com.android.zubanx.data.remote.api.DictionaryApiService
import com.android.zubanx.data.remote.dto.DictionaryResponseDto
import com.android.zubanx.data.remote.dto.MeaningDto
import com.android.zubanx.data.remote.dto.DefinitionDto
import com.android.zubanx.domain.model.DictionaryEntry
import com.android.zubanx.domain.repository.DictionaryRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LookupWordUseCaseTest {

    private val apiService = mockk<DictionaryApiService>()
    private val repository = mockk<DictionaryRepository>(relaxed = true)
    private val useCase = LookupWordUseCase(apiService, repository)

    @Test
    fun `invoke returns cached result without calling API`() = runTest {
        val cached = DictionaryEntry(
            word = "hello", language = "en",
            definition = "A greeting", timestamp = 1000L
        )
        coEvery { repository.getCached("hello", "en") } returns cached
        val result = useCase("hello", "en")
        assertTrue(result is NetworkResult.Success)
        assertEquals("hello", (result as NetworkResult.Success).data.word)
        coVerify(exactly = 0) { apiService.lookup(any(), any()) }
    }

    @Test
    fun `invoke calls API on cache miss and saves result`() = runTest {
        coEvery { repository.getCached("run", "en") } returns null
        coEvery { apiService.lookup("run", "en") } returns NetworkResult.Success(
            DictionaryResponseDto(
                word = "run",
                phonetic = "/rʌn/",
                meanings = listOf(
                    MeaningDto(
                        partOfSpeech = "verb",
                        definitions = listOf(DefinitionDto(definition = "Move at speed"))
                    )
                )
            )
        )
        val result = useCase("run", "en")
        assertTrue(result is NetworkResult.Success)
        assertEquals("run", (result as NetworkResult.Success).data.word)
        coVerify(exactly = 1) { repository.saveToCache(any()) }
    }

    @Test
    fun `invoke with blank word returns error`() = runTest {
        val result = useCase("  ", "en")
        assertTrue(result is NetworkResult.Error)
        coVerify(exactly = 0) { apiService.lookup(any(), any()) }
    }

    @Test
    fun `invoke propagates API error`() = runTest {
        coEvery { repository.getCached("xyz", "en") } returns null
        coEvery { apiService.lookup("xyz", "en") } returns NetworkResult.Error("Not found")
        val result = useCase("xyz", "en")
        assertTrue(result is NetworkResult.Error)
        coVerify(exactly = 0) { repository.saveToCache(any()) }
    }
}
