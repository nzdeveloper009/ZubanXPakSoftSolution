package com.android.zubanx.data.mlkit

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import timber.log.Timber
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * On-device translation service backed by ML Kit Translate.
 *
 * Language packs are downloaded on demand via [downloadModelIfNeeded].
 * Translation is performed offline after the pack is available.
 *
 * This service is entirely offline — it requires no network access once packs
 * are downloaded. Registered in [mlKitModule] as a `single`.
 *
 * Usage pattern for callers:
 * ```kotlin
 * mlKitTranslateService.downloadModelIfNeeded("en", "es")
 * val translated = mlKitTranslateService.translate(text, "en", "es")
 * ```
 */
class MlKitTranslateService {

    /**
     * Downloads the ML Kit language model for the given [sourceLang]→[targetLang] pair
     * if it is not already present on the device.
     *
     * Language codes follow BCP-47 format (e.g. `"en"`, `"es"`, `"fr"`).
     * ML Kit maps these via [TranslateLanguage].
     *
     * @throws Exception if the download fails.
     */
    suspend fun downloadModelIfNeeded(sourceLang: String, targetLang: String) {
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.fromLanguageTag(sourceLang) ?: TranslateLanguage.ENGLISH)
            .setTargetLanguage(TranslateLanguage.fromLanguageTag(targetLang) ?: TranslateLanguage.SPANISH)
            .build()
        val translator = Translation.getClient(options)
        val conditions = DownloadConditions.Builder().build()

        suspendCoroutine<Unit> { cont ->
            translator.downloadModelIfNeeded(conditions)
                .addOnSuccessListener {
                    Timber.d("MlKitTranslateService: model downloaded for $sourceLang→$targetLang")
                    cont.resume(Unit)
                }
                .addOnFailureListener { exception ->
                    Timber.e(exception, "MlKitTranslateService: model download failed")
                    cont.resumeWithException(exception)
                }
        }

        translator.close()
    }

    /**
     * Translates [text] from [sourceLang] to [targetLang] using the on-device ML Kit model.
     *
     * Call [downloadModelIfNeeded] first if the model may not be present. If the model
     * is not downloaded, ML Kit will attempt an automatic download (requires network).
     *
     * @throws Exception if the translation fails.
     */
    suspend fun translate(text: String, sourceLang: String, targetLang: String): String {
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.fromLanguageTag(sourceLang) ?: TranslateLanguage.ENGLISH)
            .setTargetLanguage(TranslateLanguage.fromLanguageTag(targetLang) ?: TranslateLanguage.SPANISH)
            .build()
        val translator = Translation.getClient(options)

        return suspendCoroutine { cont ->
            translator.translate(text)
                .addOnSuccessListener { translatedText ->
                    cont.resume(translatedText)
                    translator.close()
                }
                .addOnFailureListener { exception ->
                    translator.close()
                    cont.resumeWithException(exception)
                }
        }
    }
}
