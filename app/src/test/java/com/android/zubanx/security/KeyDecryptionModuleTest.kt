package com.android.zubanx.security

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyDecryptionModuleTest {

    private val remoteConfig: FirebaseRemoteConfig = mockk(relaxed = true)
    private val module = KeyDecryptionModule(remoteConfig)

    @Test
    fun `getDecryptedKey returns empty string when remote config value is blank`() {
        every { remoteConfig.getString(any()) } returns ""
        val result = module.getDecryptedKey("openai_key_enc")
        assertEquals("", result)
    }

    @Test
    fun `getDecryptedKey calls remoteConfig getString with the given key name`() {
        every { remoteConfig.getString("gemini_key_enc") } returns ""
        module.getDecryptedKey("gemini_key_enc")
        verify { remoteConfig.getString("gemini_key_enc") }
    }

    @Test
    fun `getDecryptedKey returns placeholder for non-blank value until AES is implemented`() {
        every { remoteConfig.getString("claude_key_enc") } returns "encryptedPayload=="
        val result = module.getDecryptedKey("claude_key_enc")
        // Stub: returns the raw value unchanged until AES cipher is wired in Plans 5-8
        assertTrue(result.isNotEmpty())
    }
}
