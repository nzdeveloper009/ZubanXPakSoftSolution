package com.android.zubanx.data.remote.api

import com.android.zubanx.core.network.NetworkResult
import com.android.zubanx.data.remote.dto.DictionaryResponseDto
import io.ktor.client.HttpClient
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class DictionaryApiServiceImplTest {

    private val client: HttpClient = mockk(relaxed = true)
    private val service: DictionaryApiService = DictionaryApiServiceImpl(client)

    @Test
    fun `lookup returns Error — stub not yet implemented`() = runTest {
        val result: NetworkResult<DictionaryResponseDto> = service.lookup(
            word = "run",
            language = "en"
        )
        assertTrue(result is NetworkResult.Error)
        val msg = (result as NetworkResult.Error).message
        assertTrue(msg.contains("not implemented", ignoreCase = true))
    }

}
