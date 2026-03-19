package com.android.zubanx.data.mlkit

import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognizer
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * On-device OCR service backed by ML Kit Text Recognition.
 *
 * This service is entirely offline — it requires no network access.
 * Registered in [mlKitModule] as a `single`.
 *
 * @param recognizer Injected [TextRecognizer] from `TextRecognition.getClient(...)`.
 */
class MlKitOcrService(
    private val recognizer: TextRecognizer
) {

    /**
     * Recognizes all text in [image] and returns it as a single concatenated [String].
     *
     * Blocks the calling coroutine until ML Kit's callback fires. The function is safe
     * to call on any dispatcher — ML Kit dispatches its callback on the main thread.
     *
     * @throws Exception if ML Kit fails to process the image.
     */
    suspend fun recognizeText(image: InputImage): String = suspendCoroutine { cont ->
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                cont.resume(visionText.text)
            }
            .addOnFailureListener { exception ->
                cont.resumeWithException(exception)
            }
    }
}
