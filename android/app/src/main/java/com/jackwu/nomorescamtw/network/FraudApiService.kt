package com.jackwu.nomorescamtw.network

import retrofit2.http.GET
import retrofit2.http.Streaming
import retrofit2.http.Url

interface FraudApiService {
    @GET
    @Streaming
    suspend fun downloadDatabase(@Url url: String): String
}
