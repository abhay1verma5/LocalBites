package com.localbites.core.common.util

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

inline fun <ResultType, RequestType> networkBoundResource(
    crossinline query: () -> Flow<ResultType>,
    crossinline fetch: suspend () -> RequestType,
    crossinline saveFetchResult: suspend (RequestType) -> Unit,
    crossinline shouldFetch: suspend (ResultType?) -> Boolean = { true },
): Flow<Resource<ResultType>> = flow {
    emit(Resource.Loading)

    // Get cached data first
    val cachedData = query().first()

    if (shouldFetch(cachedData)) {
        // Emit cached data while loading fresh data
        if (cachedData != null) {
            emit(Resource.Success(cachedData))
        }

        try {
            val fetchResult = fetch()
            saveFetchResult(fetchResult)
            // Emit fresh data from cache after saving
            emitAll(query().map { Resource.Success(it) })
        } catch (e: Exception) {
            // On failure: emit cached data if available, otherwise error
            if (cachedData != null) {
                emitAll(query().map { Resource.Success(it) })
            } else {
                emit(Resource.Error(e.localizedMessage ?: "Network error"))
            }
        }
    } else {
        // Cache is fresh enough, emit it directly
        emitAll(query().map { Resource.Success(it) })
    }
}
