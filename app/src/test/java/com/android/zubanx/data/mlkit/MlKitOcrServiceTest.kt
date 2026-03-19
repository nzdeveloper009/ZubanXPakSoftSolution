package com.android.zubanx.data.mlkit

import com.google.mlkit.vision.text.TextRecognizer
import io.mockk.mockk
import org.junit.Assert.assertNotNull
import org.junit.Test

class MlKitOcrServiceTest {

    private val recognizer: TextRecognizer = mockk(relaxed = true)
    private val service = MlKitOcrService(recognizer)

    @Test
    fun `MlKitOcrService instantiates with a TextRecognizer`() {
        assertNotNull(service)
    }

    @Test
    fun `MlKitOcrService exposes a recognizeText function`() {
        // Verifies the function exists with the expected signature at compile time.
        // Actual ML Kit execution requires a real InputImage and is tested on-device.
        val fn = MlKitOcrService::recognizeText
        assertNotNull(fn)
    }
}
