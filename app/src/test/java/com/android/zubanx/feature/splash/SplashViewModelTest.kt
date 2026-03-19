package com.android.zubanx.feature.splash

import app.cash.turbine.test
import com.android.zubanx.data.local.datastore.AppPreferences
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
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SplashViewModelTest {

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
    fun `Init with onboardingComplete false emits NavigateToOnboarding after delay`() = runTest {
        every { prefs.onboardingComplete } returns flowOf(false)
        val vm = SplashViewModel(prefs)

        vm.effect.test {
            vm.onEvent(SplashContract.Event.Init)
            testDispatcher.scheduler.advanceUntilIdle()
            val effect = awaitItem()
            assertEquals(SplashContract.Effect.NavigateToOnboarding, effect)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Init with onboardingComplete true emits NavigateToHome after delay`() = runTest {
        every { prefs.onboardingComplete } returns flowOf(true)
        val vm = SplashViewModel(prefs)

        vm.effect.test {
            vm.onEvent(SplashContract.Event.Init)
            testDispatcher.scheduler.advanceUntilIdle()
            val effect = awaitItem()
            assertEquals(SplashContract.Effect.NavigateToHome, effect)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
