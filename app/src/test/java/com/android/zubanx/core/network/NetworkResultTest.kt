package com.android.zubanx.core.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkResultTest {

    @Test
    fun `Success holds data`() {
        val result: NetworkResult<String> = NetworkResult.Success("hello")
        assertTrue(result is NetworkResult.Success)
        assertEquals("hello", (result as NetworkResult.Success).data)
    }

    @Test
    fun `Error holds message and null code by default`() {
        val result: NetworkResult<Nothing> = NetworkResult.Error("oops")
        assertTrue(result is NetworkResult.Error)
        val error = result as NetworkResult.Error
        assertEquals("oops", error.message)
        assertEquals(null, error.code)
    }

    @Test
    fun `Error holds message and explicit code`() {
        val result: NetworkResult<Nothing> = NetworkResult.Error("not found", 404)
        assertEquals(404, (result as NetworkResult.Error).code)
    }

    @Test
    fun `NetworkResult is covariant`() {
        val result: NetworkResult<String> = NetworkResult.Success("test")
        val covariant: NetworkResult<Any> = result
        assertTrue(covariant is NetworkResult.Success)
    }
}
