package com.android.zubanx.core.network

sealed interface NetworkResult<out T> {
    data class Success<out T>(val data: T) : NetworkResult<T>
    data class Error(val message: String, val code: Int? = null) : NetworkResult<Nothing>
}
