package com.android.zubanx.data.remote.api

import com.android.zubanx.core.network.NetworkResult
import com.android.zubanx.data.remote.dto.AiExpertResponseDto
import io.ktor.client.HttpClient
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class AiExpertServiceImplTest {

    private val client: HttpClient = mockk(relaxed = true)
    private val service: AiExpertService = AiExpertServiceImpl(client)

    @Test
    fun `ask with GPT expert returns Error — stub not yet implemented`() = runTest {
        val result: NetworkResult<AiExpertResponseDto> = service.ask(
            expert = "GPT",
            prompt = "Translate 'Hello' to Spanish"
        )
        assertTrue(result is NetworkResult.Error)
        assertTrue((result as NetworkResult.Error).message.contains("not implemented", ignoreCase = true))
    }

    @Test
    fun `ask with GEMINI expert returns Error — stub not yet implemented`() = runTest {
        val result = service.ask(expert = "GEMINI", prompt = "Explain 'run'")
        assertTrue(result is NetworkResult.Error)
        assertTrue((result as NetworkResult.Error).message.contains("not implemented", ignoreCase = true))
    }

    @Test
    fun `ask with CLAUDE expert returns Error — stub not yet implemented`() = runTest {
        val result = service.ask(expert = "CLAUDE", prompt = "Translate 'cat'")
        assertTrue(result is NetworkResult.Error)
        assertTrue((result as NetworkResult.Error).message.contains("not implemented", ignoreCase = true))
    }

    @Test
    fun `ask with DEFAULT expert returns Error — stub not yet implemented`() = runTest {
        val result = service.ask(expert = "DEFAULT", prompt = "Translate 'dog'")
        assertTrue(result is NetworkResult.Error)
        assertTrue((result as NetworkResult.Error).message.contains("not implemented", ignoreCase = true))
    }

    @Test
    fun `AiExpertServiceImpl satisfies AiExpertService interface`() {
        val impl: AiExpertService = AiExpertServiceImpl(client)
        assertTrue(impl is AiExpertServiceImpl)
    }
}
