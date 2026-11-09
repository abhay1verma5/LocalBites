package com.localbites.core.common.util

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
