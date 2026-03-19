package com.android.zubanx.data.mlkit

import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognizer
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * On-device OCR service backed by ML Kit Text Recognition.
 *
 * This service is entirely offline — it requires no network access.
 * Registered in [mlKitModule] as a `single`.
 *
 * Note: [recognizer] lifecycle is managed by the DI container. The Koin module
 * must call `recognizer.close()` when the scope is destroyed.
 *
 * @param recognizer Injected [TextRecognizer] from `TextRecognition.getClient(...)`.
 */
class MlKitOcrService(
    private val recognizer: TextRecognizer
) {

    /**
     * Recognizes all text in [image] and returns it as a single concatenated [String].
     *
     * Suspends until ML Kit's callback fires. Supports cooperative cancellation —
     * if the calling coroutine is cancelled, the suspension is lifted immediately
     * (ML Kit will still complete internally).
     *
     * @throws Exception if ML Kit fails to process the image.
     */
    suspend fun recognizeText(image: InputImage): String = suspendCancellableCoroutine { cont ->
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                cont.resume(visionText.text)
            }
            .addOnFailureListener { exception ->
                cont.resumeWithException(exception)
            }
    }
}
