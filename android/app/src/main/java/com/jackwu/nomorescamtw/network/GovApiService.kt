package com.jackwu.nomorescamtw.network

import retrofit2.http.GET

interface GovApiService {
    @GET("160055")
    suspend fun getFraudDatasetMetadata(): GovDatasetResponse
}

data class GovDatasetResponse(
    val success: Boolean,
    val result: GovResult?
)

data class GovResult(
    val distribution: List<GovDistribution>?
)

data class GovDistribution(
    val resourceDownloadUrl: String?
)
