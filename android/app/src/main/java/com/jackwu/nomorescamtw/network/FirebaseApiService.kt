package com.jackwu.nomorescamtw.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path

interface FirebaseApiService {
    @GET("stats/{statName}.json")
    suspend fun getStat(@Path("statName") statName: String): Response<Int>

    @PUT("stats/{statName}.json")
    suspend fun setStat(@Path("statName") statName: String, @Body count: Int): Response<Int>
}
