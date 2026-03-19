package com.android.zubanx.tts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TtsStateTest {

    @Test
    fun `TtsState Idle is the initial state sentinel`() {
        val state: TtsState = TtsState.Idle
        assertTrue(state is TtsState.Idle)
    }

    @Test
    fun `TtsState Speaking holds the text being spoken`() {
        val state: TtsState = TtsState.Speaking(text = "Hello")
        assertTrue(state is TtsState.Speaking)
        assertEquals("Hello", (state as TtsState.Speaking).text)
    }

    @Test
    fun `TtsState Error holds an error message`() {
        val state: TtsState = TtsState.Error(message = "TTS init failed")
        assertTrue(state is TtsState.Error)
        assertEquals("TTS init failed", (state as TtsState.Error).message)
    }
}
