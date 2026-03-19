package com.android.zubanx.core.network

import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.Logging
import org.junit.Assert.assertNotNull
import org.junit.Test

class KtorClientFactoryTest {

    @Test
    fun `create returns non-null HttpClient`() {
        val client = KtorClientFactory.create()
        assertNotNull(client)
        client.close()
    }

    @Test
    fun `create installs ContentNegotiation plugin`() {
        val client = KtorClientFactory.create()
        assertNotNull(client.pluginOrNull(ContentNegotiation))
        client.close()
    }

    @Test
    fun `create installs Logging plugin`() {
        val client = KtorClientFactory.create()
        assertNotNull(client.pluginOrNull(Logging))
        client.close()
    }

    @Test
    fun `create produces distinct clients on each call`() {
        val a = KtorClientFactory.create()
        val b = KtorClientFactory.create()
        assert(a !== b)
        a.close()
        b.close()
    }
}
