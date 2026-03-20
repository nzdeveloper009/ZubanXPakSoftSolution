package com.android.zubanx.feature.translate

import app.cash.turbine.test
import com.android.zubanx.core.network.NetworkResult
import com.android.zubanx.data.local.datastore.AppPreferences
import com.android.zubanx.data.remote.dto.TranslateResponseDto
import com.android.zubanx.domain.usecase.translate.AddFavouriteFromTranslationUseCase
import com.android.zubanx.domain.usecase.translate.DeleteTranslationUseCase
import com.android.zubanx.domain.usecase.translate.GetTranslationHistoryUseCase
import com.android.zubanx.domain.usecase.translate.TranslateUseCase
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
class TranslateViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val translateUseCase = mockk<TranslateUseCase>()
    private val historyUseCase = mockk<GetTranslationHistoryUseCase>()
    private val deleteUseCase = mockk<DeleteTranslationUseCase>(relaxed = true)
    private val addFavouriteUseCase = mockk<AddFavouriteFromTranslationUseCase>(relaxed = true)
    private val appPreferences = mockk<AppPreferences>()

    private lateinit var viewModel: TranslateViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { appPreferences.selectedExpert } returns flowOf("DEFAULT")
        every { appPreferences.sourceLang } returns flowOf("en")
        every { appPreferences.targetLang } returns flowOf("ur")
        every { historyUseCase() } returns flowOf(emptyList())
        viewModel = TranslateViewModel(
            translateUseCase, historyUseCase, deleteUseCase, addFavouriteUseCase, appPreferences
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is Idle`() {
        assertTrue(viewModel.state.value is TranslateContract.State.Idle)
    }

    @Test
    fun `TranslateClicked with text transitions to Success`() = runTest {
        coEvery { translateUseCase("Hello", any(), any(), any()) } returns NetworkResult.Success(
            TranslateResponseDto(translatedText = "ہیلو", sourceLang = "en", targetLang = "ur")
        )
        viewModel.onEvent(TranslateContract.Event.InputChanged("Hello"))
        viewModel.onEvent(TranslateContract.Event.TranslateClicked)
        dispatcher.scheduler.advanceUntilIdle()
        val state = viewModel.state.value
        assertTrue(state is TranslateContract.State.Success)
        assertEquals("ہیلو", (state as TranslateContract.State.Success).translatedText)
    }

    @Test
    fun `TranslateClicked with blank text emits Error state`() = runTest {
        viewModel.onEvent(TranslateContract.Event.InputChanged(""))
        viewModel.onEvent(TranslateContract.Event.TranslateClicked)
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.state.value is TranslateContract.State.Error)
    }

    @Test
    fun `MicResult sets input and triggers translate`() = runTest {
        coEvery { translateUseCase("Spoken text", any(), any(), any()) } returns NetworkResult.Success(
            TranslateResponseDto(translatedText = "بولا گیا متن", sourceLang = "en", targetLang = "ur")
        )
        viewModel.onEvent(TranslateContract.Event.MicResult("Spoken text"))
        dispatcher.scheduler.advanceUntilIdle()
        val state = viewModel.state.value
        assertTrue(state is TranslateContract.State.Success)
    }

    @Test
    fun `ClearInput resets state to Idle`() = runTest {
        viewModel.onEvent(TranslateContract.Event.ClearInput)
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.state.value is TranslateContract.State.Idle)
    }

    @Test
    fun `CopyTranslation emits CopyToClipboard effect when in Success state`() = runTest {
        coEvery { translateUseCase(any(), any(), any(), any()) } returns NetworkResult.Success(
            TranslateResponseDto(translatedText = "Hola", sourceLang = "en", targetLang = "es")
        )
        viewModel.onEvent(TranslateContract.Event.InputChanged("Hello"))
        viewModel.onEvent(TranslateContract.Event.TranslateClicked)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.effect.test {
            viewModel.onEvent(TranslateContract.Event.CopyTranslation)
            dispatcher.scheduler.advanceUntilIdle()
            val effect = awaitItem()
            assertTrue(effect is TranslateContract.Effect.CopyToClipboard)
            assertEquals("Hola", (effect as TranslateContract.Effect.CopyToClipboard).text)
            cancelAndConsumeRemainingEvents()
        }
    }
}
