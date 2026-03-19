package com.android.zubanx.tts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SttStateTest {

    @Test
    fun `SttState Idle is the initial state`() {
        val state: SttState = SttState.Idle
        assertTrue(state is SttState.Idle)
    }

    @Test
    fun `SttState Listening represents active recording`() {
        val state: SttState = SttState.Listening
        assertTrue(state is SttState.Listening)
    }

    @Test
    fun `SttState Result holds recognized text`() {
        val state: SttState = SttState.Result(text = "Translate this")
        assertTrue(state is SttState.Result)
        assertEquals("Translate this", (state as SttState.Result).text)
    }

    @Test
    fun `SttState Error holds an error message`() {
        val state: SttState = SttState.Error(message = "Permission denied")
        assertTrue(state is SttState.Error)
        assertEquals("Permission denied", (state as SttState.Error).message)
    }
}
