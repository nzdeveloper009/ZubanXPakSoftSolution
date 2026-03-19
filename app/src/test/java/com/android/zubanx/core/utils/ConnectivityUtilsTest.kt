package com.android.zubanx.core.utils

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import io.mockk.every
import io.mockk.mockk
import org.junit.Test

class ConnectivityUtilsTest {

    @Test
    fun `isOnline returns true when network has INTERNET capability`() {
        val connectivityManager = mockk<ConnectivityManager>()
        val network = mockk<Network>()
        val capabilities = mockk<NetworkCapabilities>()
        every { connectivityManager.activeNetwork } returns network
        every { connectivityManager.getNetworkCapabilities(network) } returns capabilities
        every { capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns true

        val utils = ConnectivityUtils(connectivityManager)

        assert(utils.isOnline())
    }

    @Test
    fun `isOnline returns false when no active network`() {
        val connectivityManager = mockk<ConnectivityManager>()
        every { connectivityManager.activeNetwork } returns null

        val utils = ConnectivityUtils(connectivityManager)

        assert(!utils.isOnline())
    }

    @Test
    fun `isOnline returns false when capabilities are null`() {
        val connectivityManager = mockk<ConnectivityManager>()
        val network = mockk<Network>()
        every { connectivityManager.activeNetwork } returns network
        every { connectivityManager.getNetworkCapabilities(network) } returns null

        val utils = ConnectivityUtils(connectivityManager)

        assert(!utils.isOnline())
    }
}
