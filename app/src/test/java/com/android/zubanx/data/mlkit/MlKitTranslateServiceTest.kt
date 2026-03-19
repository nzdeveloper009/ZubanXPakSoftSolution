package com.android.zubanx.data.mlkit

import org.junit.Assert.assertNotNull
import org.junit.Test

class MlKitTranslateServiceTest {

    private val service = MlKitTranslateService()

    @Test
    fun `MlKitTranslateService instantiates`() {
        assertNotNull(service)
    }

    @Test
    fun `MlKitTranslateService exposes translate function`() {
        val fn = MlKitTranslateService::translate
        assertNotNull(fn)
    }

    @Test
    fun `MlKitTranslateService exposes downloadModelIfNeeded function`() {
        val fn = MlKitTranslateService::downloadModelIfNeeded
        assertNotNull(fn)
    }
}
