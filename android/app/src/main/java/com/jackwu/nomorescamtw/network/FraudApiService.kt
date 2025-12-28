package com.jackwu.nomorescamtw.network

import retrofit2.http.GET
import retrofit2.http.Streaming

interface FraudApiService {
    @GET("api/v1/no-auth/resource/api/dataset/033197D4-70F4-45EB-9FB8-6D83532B999A/resource/D24B474A-9239-44CA-8177-56D7859A31F6/download")
    @Streaming
    suspend fun downloadDatabase(): String
}
