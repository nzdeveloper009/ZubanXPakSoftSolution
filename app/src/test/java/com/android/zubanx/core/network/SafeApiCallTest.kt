package com.android.zubanx.core.network

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class SafeApiCallTest {

    @Test
    fun `returns Success when block succeeds`() = runTest {
        val result = safeApiCall { "translation" }
        assertTrue(result is NetworkResult.Success)
        assertEquals("translation", (result as NetworkResult.Success).data)
    }

    @Test
    fun `returns Error when IOException is thrown`() = runTest {
        val result = safeApiCall<String> { throw IOException("network error") }
        assertTrue(result is NetworkResult.Error)
        assertTrue((result as NetworkResult.Error).message.contains("network error"))
        assertEquals(null, result.code)
    }

    @Test
    fun `returns Error when generic Exception is thrown`() = runTest {
        val result = safeApiCall<String> { throw RuntimeException("unexpected") }
        assertTrue(result is NetworkResult.Error)
        assertTrue((result as NetworkResult.Error).message.contains("unexpected"))
    }

    @Test
    fun `returns Error for HTTP exception pattern`() = runTest {
        val result = safeApiCall<String> { throw Exception("HTTP 400: Bad Request") }
        assertTrue(result is NetworkResult.Error)
    }
}
