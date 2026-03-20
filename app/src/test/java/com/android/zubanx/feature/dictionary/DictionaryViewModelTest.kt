package com.android.zubanx.feature.dictionary

import com.android.zubanx.core.network.NetworkResult
import com.android.zubanx.domain.model.DictionaryEntry
import com.android.zubanx.domain.usecase.dictionary.GetDictionaryHistoryUseCase
import com.android.zubanx.domain.usecase.dictionary.LookupWordUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DictionaryViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val lookupUseCase = mockk<LookupWordUseCase>()
    private val historyUseCase = mockk<GetDictionaryHistoryUseCase>()

    private lateinit var viewModel: DictionaryViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { historyUseCase() } returns flowOf(emptyList())
        viewModel = DictionaryViewModel(lookupUseCase, historyUseCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is Idle`() {
        assertTrue(viewModel.state.value is DictionaryContract.State.Idle)
    }

    @Test
    fun `SearchClicked with blank query emits Error`() = runTest {
        viewModel.onEvent(DictionaryContract.Event.QueryChanged(""))
        viewModel.onEvent(DictionaryContract.Event.SearchClicked)
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.state.value is DictionaryContract.State.Error)
    }

    @Test
    fun `SearchClicked with valid word transitions to Success`() = runTest {
        val entry = DictionaryEntry(
            word = "hello", language = "en",
            definition = "[exclamation]\n1. Used as a greeting",
            phonetic = "/həˈloʊ/",
            timestamp = 1000L
        )
        coEvery { lookupUseCase("hello", "en") } returns NetworkResult.Success(entry)
        viewModel.onEvent(DictionaryContract.Event.QueryChanged("hello"))
        viewModel.onEvent(DictionaryContract.Event.SearchClicked)
        dispatcher.scheduler.advanceUntilIdle()
        val state = viewModel.state.value
        assertTrue(state is DictionaryContract.State.Success)
        assertEquals("hello", (state as DictionaryContract.State.Success).entry.word)
    }

    @Test
    fun `MicResult triggers search`() = runTest {
        val entry = DictionaryEntry(
            word = "run", language = "en",
            definition = "[verb]\n1. Move at speed",
            timestamp = 2000L
        )
        coEvery { lookupUseCase("run", "en") } returns NetworkResult.Success(entry)
        viewModel.onEvent(DictionaryContract.Event.MicResult("run"))
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.state.value is DictionaryContract.State.Success)
    }

    @Test
    fun `ClearSearch resets state to Idle`() = runTest {
        viewModel.onEvent(DictionaryContract.Event.ClearSearch)
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.state.value is DictionaryContract.State.Idle)
    }
}
