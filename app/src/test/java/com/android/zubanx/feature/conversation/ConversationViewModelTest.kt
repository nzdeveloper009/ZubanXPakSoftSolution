// app/src/test/java/com/android/zubanx/feature/conversation/ConversationViewModelTest.kt
package com.android.zubanx.feature.conversation

import com.android.zubanx.core.network.NetworkResult
import com.android.zubanx.data.remote.dto.TranslateResponseDto
import com.android.zubanx.domain.usecase.conversation.ConversationTranslateUseCase
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ConversationViewModelTest {

    private val translateUseCase: ConversationTranslateUseCase = mockk()
    private lateinit var viewModel: ConversationViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = ConversationViewModel(translateUseCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has no messages`() = runTest {
        val state = viewModel.state.first()
        assertTrue(state is ConversationContract.State.Active)
        assertTrue((state as ConversationContract.State.Active).messages.isEmpty())
    }

    @Test
    fun `MicResultA translates and adds message to list`() = runTest {
        coEvery { translateUseCase("hello", "en", "ur") } returns
            NetworkResult.Success(TranslateResponseDto("سلام", "en", "ur"))

        viewModel.onEvent(ConversationContract.Event.MicResultA("hello"))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.first() as ConversationContract.State.Active
        assertEquals(1, state.messages.size)
        assertEquals("hello", state.messages[0].originalText)
        assertEquals("سلام", state.messages[0].translatedText)
        assertEquals(ConversationContract.SpeakerSide.A, state.messages[0].speakerSide)
    }

    @Test
    fun `MicResultB translates using reversed language direction`() = runTest {
        coEvery { translateUseCase("marhaba", "ur", "en") } returns
            NetworkResult.Success(TranslateResponseDto("Hello", "ur", "en"))

        viewModel.onEvent(ConversationContract.Event.MicResultB("marhaba"))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.first() as ConversationContract.State.Active
        assertEquals(1, state.messages.size)
        assertEquals("marhaba", state.messages[0].originalText)
        assertEquals("Hello", state.messages[0].translatedText)
        assertEquals(ConversationContract.SpeakerSide.B, state.messages[0].speakerSide)
    }

    @Test
    fun `ClearConversation empties the message list`() = runTest {
        coEvery { translateUseCase(any(), any(), any()) } returns
            NetworkResult.Success(TranslateResponseDto("سلام", "en", "ur"))

        viewModel.onEvent(ConversationContract.Event.MicResultA("hello"))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(ConversationContract.Event.ClearConversation)

        val state = viewModel.state.first() as ConversationContract.State.Active
        assertTrue(state.messages.isEmpty())
    }
}
