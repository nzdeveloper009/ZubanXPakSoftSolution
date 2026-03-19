package com.android.zubanx.data.remote.api

import com.android.zubanx.core.network.NetworkResult
import com.android.zubanx.data.remote.dto.TranslateResponseDto
import io.ktor.client.HttpClient
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class TranslateApiServiceImplTest {

    private val client: HttpClient = mockk(relaxed = true)
    private val service: TranslateApiService = TranslateApiServiceImpl(client)

    @Test
    fun `translate returns Error — stub not yet implemented`() = runTest {
        val result: NetworkResult<TranslateResponseDto> = service.translate(
            text = "Hello",
            sourceLang = "en",
            targetLang = "es"
        )
        assertTrue(result is NetworkResult.Error)
        val msg = (result as NetworkResult.Error).message
        assertTrue(msg.contains("not implemented", ignoreCase = true))
    }

    @Test
    fun `TranslateApiServiceImpl satisfies TranslateApiService interface`() {
        val impl: TranslateApiService = TranslateApiServiceImpl(client)
        assertTrue(impl is TranslateApiServiceImpl)
    }
}
