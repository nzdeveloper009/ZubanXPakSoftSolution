package com.android.zubanx.feature.onboarding

import app.cash.turbine.test
import com.android.zubanx.data.local.datastore.AppPreferences
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val prefs: AppPreferences = mockk(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has currentPage 0 and totalPages 3`() {
        val vm = OnboardingViewModel(prefs)
        assertEquals(0, vm.state.value.currentPage)
        assertEquals(3, vm.state.value.totalPages)
    }

    @Test
    fun `PageChanged event updates currentPage in state`() {
        val vm = OnboardingViewModel(prefs)
        vm.onEvent(OnboardingContract.Event.PageChanged(2))
        assertEquals(2, vm.state.value.currentPage)
    }

    @Test
    fun `DoneClicked sets onboardingComplete true and emits NavigateToHome`() = runTest {
        val vm = OnboardingViewModel(prefs)
        vm.effect.test {
            vm.onEvent(OnboardingContract.Event.DoneClicked)
            testDispatcher.scheduler.advanceUntilIdle()
            coVerify { prefs.setOnboardingComplete(true) }
            val effect = awaitItem()
            assertEquals(OnboardingContract.Effect.NavigateToHome, effect)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `SkipClicked sets onboardingComplete true and emits NavigateToHome`() = runTest {
        val vm = OnboardingViewModel(prefs)
        vm.effect.test {
            vm.onEvent(OnboardingContract.Event.SkipClicked)
            testDispatcher.scheduler.advanceUntilIdle()
            coVerify { prefs.setOnboardingComplete(true) }
            val effect = awaitItem()
            assertEquals(OnboardingContract.Effect.NavigateToHome, effect)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
