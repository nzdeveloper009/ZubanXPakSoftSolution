package com.android.zubanx.feature.phrases

import com.android.zubanx.core.network.NetworkResult
import com.android.zubanx.data.remote.dto.TranslateResponseDto
import com.android.zubanx.domain.usecase.phrases.TranslatePhraseUseCase
import com.android.zubanx.feature.phrases.data.PhrasesData
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PhrasesCategoryViewModelTest {

    private val translateUseCase: TranslatePhraseUseCase = mockk()
    private lateinit var viewModel: PhrasesCategoryViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = PhrasesCategoryViewModel(translateUseCase, "dining")
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state loads dining category with English phrases`() = runTest {
        val state = viewModel.state.first() as PhrasesCategoryContract.State.Active
        assertEquals("dining", state.category.id)
        assertEquals(PhrasesData.phrases[state.category]!!.size, state.displayPhrases.size)
        assertNull(state.expandedIndex)
    }

    @Test
    fun `ExpandPhrase sets expandedIndex`() = runTest {
        viewModel.onEvent(PhrasesCategoryContract.Event.ExpandPhrase(2))
        val state = viewModel.state.first() as PhrasesCategoryContract.State.Active
        assertEquals(2, state.expandedIndex)
    }

    @Test
    fun `CollapsePhrase clears expandedIndex`() = runTest {
        viewModel.onEvent(PhrasesCategoryContract.Event.ExpandPhrase(2))
        viewModel.onEvent(PhrasesCategoryContract.Event.CollapsePhrase)
        val state = viewModel.state.first() as PhrasesCategoryContract.State.Active
        assertNull(state.expandedIndex)
    }

    @Test
    fun `SwapLanguages reverses source and target`() = runTest {
        val before = viewModel.state.first() as PhrasesCategoryContract.State.Active
        val srcBefore = before.langSource.code
        val tgtBefore = before.langTarget.code

        viewModel.onEvent(PhrasesCategoryContract.Event.SwapLanguages)
        testDispatcher.scheduler.advanceUntilIdle()

        val after = viewModel.state.first() as PhrasesCategoryContract.State.Active
        assertEquals(tgtBefore, after.langSource.code)
        assertEquals(srcBefore, after.langTarget.code)
        assertNull(after.expandedIndex)
    }

    @Test
    fun `ExpandPhrase with same index collapses it`() = runTest {
        viewModel.onEvent(PhrasesCategoryContract.Event.ExpandPhrase(1))
        viewModel.onEvent(PhrasesCategoryContract.Event.ExpandPhrase(1))
        val state = viewModel.state.first() as PhrasesCategoryContract.State.Active
        assertNull(state.expandedIndex)
    }

    @Test
    fun `translation result is cached and stored`() = runTest {
        coEvery { translateUseCase(any(), "en", "ur") } returns
            NetworkResult.Success(TranslateResponseDto("ہیلو", "en", "ur"))

        viewModel.onEvent(PhrasesCategoryContract.Event.ExpandPhrase(0))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.first() as PhrasesCategoryContract.State.Active
        assertTrue(state.translationCache.containsKey("en:ur:0"))
        assertEquals(0, state.loadingIndices.size)
    }

    @Test
    fun `failed translation adds index to errorIndices`() = runTest {
        coEvery { translateUseCase(any(), "en", "ur") } returns
            NetworkResult.Error("Network error")

        viewModel.onEvent(PhrasesCategoryContract.Event.ExpandPhrase(0))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.first() as PhrasesCategoryContract.State.Active
        assertTrue(state.errorIndices.contains(0))
        assertEquals(0, state.loadingIndices.size)
    }

    @Test
    fun `SpeakPhrase sends SpeakText effect when translation is cached`() = runTest {
        coEvery { translateUseCase(any(), "en", "ur") } returns
            NetworkResult.Success(TranslateResponseDto("ہیلو", "en", "ur"))

        viewModel.onEvent(PhrasesCategoryContract.Event.ExpandPhrase(0))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(PhrasesCategoryContract.Event.SpeakPhrase(0))
        testDispatcher.scheduler.advanceUntilIdle()

        val effect = viewModel.effect.first()
        assertTrue(effect is PhrasesCategoryContract.Effect.SpeakText)
        assertEquals("ہیلو", (effect as PhrasesCategoryContract.Effect.SpeakText).text)
        assertEquals("ur", effect.langCode)
    }

    @Test
    fun `CopyPhrase sends CopyToClipboard effect when translation is cached`() = runTest {
        coEvery { translateUseCase(any(), "en", "ur") } returns
            NetworkResult.Success(TranslateResponseDto("ہیلو", "en", "ur"))

        viewModel.onEvent(PhrasesCategoryContract.Event.ExpandPhrase(0))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(PhrasesCategoryContract.Event.CopyPhrase(0))
        testDispatcher.scheduler.advanceUntilIdle()

        val effect = viewModel.effect.first()
        assertTrue(effect is PhrasesCategoryContract.Effect.CopyToClipboard)
        assertEquals("ہیلو", (effect as PhrasesCategoryContract.Effect.CopyToClipboard).text)
    }
}
