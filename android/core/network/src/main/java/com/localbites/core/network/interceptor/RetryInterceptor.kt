package com.localbites.core.network.interceptor

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

class RetryInterceptor(private val maxRetries: Int = 3) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        // Skip retry for non-idempotent methods
        if (request.method in listOf("POST", "PUT", "DELETE", "PATCH")) {
            return chain.proceed(request)
        }

        var lastException: IOException? = null
        var lastResponse: Response? = null

        for (attempt in 0..maxRetries) {
            try {
                lastResponse?.close()
                val response = chain.proceed(request)

                // Don't retry on client errors (4xx) or success (2xx/3xx)
                if (response.code < 500) {
                    return response
                }

                // Retry on 5xx server errors
                if (attempt < maxRetries) {
                    lastResponse = response
                    val backoffMs = 1000L * (1 shl attempt) // 1s, 2s, 4s
                    Thread.sleep(backoffMs)
                } else {
                    return response
                }
            } catch (e: IOException) {
                lastException = e
                if (attempt < maxRetries) {
                    val backoffMs = 1000L * (1 shl attempt)
                    Thread.sleep(backoffMs)
                }
            }
        }

        throw lastException ?: IOException("Retry failed after $maxRetries attempts")
    }
}
