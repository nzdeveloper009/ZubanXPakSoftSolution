package com.android.zubanx.security

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import timber.log.Timber

/**
 * Fetches and decrypts API keys stored as AES-encrypted strings in Firebase Remote Config.
 *
 * Keys are never stored in plaintext on-device. Each key is fetched fresh for each
 * network call via [getDecryptedKey].
 *
 * STUB: The AES cipher wiring is added in Plans 5-8 when actual API keys are provisioned.
 * The current implementation returns the raw Remote Config value unchanged.
 *
 * @param remoteConfig Injected Firebase Remote Config singleton.
 */
class KeyDecryptionModule(
    private val remoteConfig: FirebaseRemoteConfig
) {

    /**
     * Returns the decrypted API key for [keyName].
     *
     * Returns an empty string if the Remote Config value is blank (not yet fetched
     * or the key is not published). Callers must guard against an empty result.
     *
     * @param keyName The Remote Config parameter name, e.g. `"openai_key_enc"`.
     */
    fun getDecryptedKey(keyName: String): String {
        val encrypted = remoteConfig.getString(keyName)
        if (encrypted.isBlank()) {
            Timber.w("KeyDecryptionModule: no value for key '$keyName' in Remote Config")
            return ""
        }
        // TODO Plan 5-8: replace with AES/GCM decryption using the master key
        // stored in Android Keystore. For now return the raw value as a placeholder.
        return encrypted
    }
}
