package com.localbites.core.network.api

import com.localbites.core.network.model.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface PaymentApi {
    @POST("payments/create/")
    suspend fun createPayment(@Body request: CreatePaymentRequest): Response<CreatePaymentResponse>

    @POST("payments/verify/")
    suspend fun verifyPayment(@Body data: Map<String, String>): Response<Map<String, String>>
}
