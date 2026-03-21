package com.android.zubanx.feature.language

import com.android.zubanx.data.local.datastore.AppPreferences
import io.mockk.coVerify
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
class LanguageViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val appPreferences = mockk<AppPreferences>(relaxed = true)
    private lateinit var viewModel: LanguageViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { appPreferences.appLanguage } returns flowOf("en")
        viewModel = LanguageViewModel(appPreferences)
    }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `initial selectedCode matches stored preference`() = runTest {
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals("en", viewModel.state.value.selectedCode)
    }

    @Test
    fun `SelectLanguage updates selectedCode in state`() = runTest {
        viewModel.onEvent(LanguageContract.Event.SelectLanguage("ur"))
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals("ur", viewModel.state.value.selectedCode)
    }

    @Test
    fun `Confirm saves selectedCode to preferences`() = runTest {
        viewModel.onEvent(LanguageContract.Event.SelectLanguage("hi"))
        viewModel.onEvent(LanguageContract.Event.Confirm)
        dispatcher.scheduler.advanceUntilIdle()
        coVerify { appPreferences.setAppLanguage("hi") }
    }
}
