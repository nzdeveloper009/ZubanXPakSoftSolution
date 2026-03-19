package com.android.zubanx.core.mvi

import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BaseViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    sealed interface TestState : UiState {
        object Idle : TestState
        data class Loaded(val value: String) : TestState
    }

    sealed class TestEvent : UiEvent {
        data class Load(val value: String) : TestEvent()
    }

    sealed class TestEffect : UiEffect {
        data class ShowToast(val msg: String) : TestEffect()
    }

    class TestViewModel : BaseViewModel<TestState, TestEvent, TestEffect>(TestState.Idle) {
        override fun onEvent(event: TestEvent) {
            when (event) {
                is TestEvent.Load -> {
                    setState { TestState.Loaded(event.value) }
                    sendEffect(TestEffect.ShowToast("Loaded: ${event.value}"))
                }
            }
        }
    }

    @Before
    fun setUp() = Dispatchers.setMain(testDispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `initial state is Idle`() = runTest {
        val vm = TestViewModel()
        assert(vm.state.value is TestState.Idle)
    }

    @Test
    fun `onEvent Load transitions state to Loaded`() = runTest {
        val vm = TestViewModel()
        vm.state.test {
            assert(awaitItem() is TestState.Idle)
            vm.onEvent(TestEvent.Load("hello"))
            testDispatcher.scheduler.advanceUntilIdle()
            val loaded = awaitItem()
            assert(loaded is TestState.Loaded && (loaded as TestState.Loaded).value == "hello")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onEvent Load sends ShowToast effect`() = runTest {
        val vm = TestViewModel()
        vm.effect.test {
            vm.onEvent(TestEvent.Load("hello"))
            testDispatcher.scheduler.advanceUntilIdle()
            val effect = awaitItem()
            assert(effect is TestEffect.ShowToast && (effect as TestEffect.ShowToast).msg == "Loaded: hello")
            cancelAndIgnoreRemainingEvents()
        }
    }
}
