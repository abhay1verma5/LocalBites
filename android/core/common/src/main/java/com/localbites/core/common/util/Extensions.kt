package com.localbites.core.common.util

import kotlinx.coroutines.delay
import retrofit2.Response

suspend fun <T> safeApiCall(apiCall: suspend () -> Response<T>): Resource<T> {
    return try {
        val response = apiCall()
        if (response.isSuccessful) {
            response.body()?.let { Resource.Success(it) }
                ?: Resource.Error("Empty response body")
        } else {
            Resource.Error(
                message = response.errorBody()?.string() ?: "Unknown error",
                code = response.code()
            )
        }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Network error")
    }
}

suspend fun <T> safeApiCallWithRetry(
    maxRetries: Int = 3,
    initialDelayMs: Long = 1000L,
    apiCall: suspend () -> Response<T>,
): Resource<T> {
    var lastResult: Resource<T> = Resource.Error("No attempts made")
    for (attempt in 0..maxRetries) {
        val result = safeApiCall(apiCall)
        if (result is Resource.Success) return result
        lastResult = result
        // Don't retry on client errors (4xx)
        if (result is Resource.Error && result.code != null && result.code in 400..499) {
            return result
        }
        if (attempt < maxRetries) {
            delay(initialDelayMs * (1 shl attempt)) // Exponential backoff
        }
    }
    return lastResult
}
