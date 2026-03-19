package com.android.zubanx.data.local.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class AppPreferencesImplTest {

    private val dataStore: DataStore<Preferences> = mockk()

    @Test
    fun `theme returns stored value`() = runTest {
        val prefs = mutablePreferencesOf(stringPreferencesKey("theme") to "DARK")
        every { dataStore.data } returns flowOf(prefs)
        assertEquals("DARK", AppPreferencesImpl(dataStore).theme.first())
    }

    @Test
    fun `isPremium defaults to false when not set`() = runTest {
        every { dataStore.data } returns flowOf(emptyPreferences())
        assertFalse(AppPreferencesImpl(dataStore).isPremium.first())
    }

    @Test
    fun `selectedExpert defaults to DEFAULT when not set`() = runTest {
        every { dataStore.data } returns flowOf(emptyPreferences())
        assertEquals("DEFAULT", AppPreferencesImpl(dataStore).selectedExpert.first())
    }
}
