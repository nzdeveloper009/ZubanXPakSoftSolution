package com.android.zubanx.core.network

import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import timber.log.Timber
import java.io.IOException

/**
 * Wraps a Ktor network call and converts exceptions into [NetworkResult].
 * Loading state is a UI/MVI concern — not modelled here.
 */
suspend fun <T> safeApiCall(block: suspend () -> T): NetworkResult<T> {
    return try {
        NetworkResult.Success(block())
    } catch (e: ClientRequestException) {
        val code = e.response.status.value
        Timber.w(e, "safeApiCall ClientRequestException: $code")
        NetworkResult.Error(e.message ?: "Client error $code", code)
    } catch (e: ServerResponseException) {
        val code = e.response.status.value
        Timber.w(e, "safeApiCall ServerResponseException: $code")
        NetworkResult.Error(e.message ?: "Server error $code", code)
    } catch (e: IOException) {
        Timber.w(e, "safeApiCall IOException")
        NetworkResult.Error(e.message ?: "IO error", null)
    } catch (e: Exception) {
        Timber.e(e, "safeApiCall unexpected exception")
        NetworkResult.Error(e.message ?: "Unknown error", null)
    }
}
