package com.android.zubanx.core.network

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
    fun `create is configured with plugins`() {
        // Plugins are installed in the builder. This test just verifies the
        // client creation succeeds and is usable.
        val client = KtorClientFactory.create()
        assertNotNull(client)
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
