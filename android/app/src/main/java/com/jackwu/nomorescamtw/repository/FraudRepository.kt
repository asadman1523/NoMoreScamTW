package com.jackwu.nomorescamtw.repository

import android.util.Log
import com.jackwu.nomorescamtw.data.FraudDao
import com.jackwu.nomorescamtw.data.FraudSite
import com.jackwu.nomorescamtw.network.FraudApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

class FraudRepository(
    private val fraudDao: FraudDao,
    private val apiService: FraudApiService
) {

    suspend fun updateDatabase(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            // 1. Fetch Remote Config
            val configUrl = "https://cdn.jsdelivr.net/gh/asadman1523/NoMoreScamTW@main/server_config.json"
            val defaultDataUrl = "https://opdadm.moi.gov.tw/api/v1/no-auth/resource/api/dataset/033197D4-70F4-45EB-9FB8-6D83532B999A/resource/D24B474A-9239-44CA-8177-56D7859A31F6/download"
            
            var targetUrl = defaultDataUrl
            
            try {
                // Use a standard HTTP request to get the config
                // Since we don't have a dedicated API service for GitHub, we can use a basic URL connection or if we have OkHttp client exposed? 
                // We passed ApiService which uses Retrofit. We can't easily retrieve the OkHttp client from it without casting.
                // Simpler: Just use java.net.URL for the config fetch since it's simple JSON.
                
                val configJson = java.net.URL(configUrl).readText()
                // Simple parser since we don't want to add Gson/Moshi just for this one field if not already there.
                // Pattern: "csv_url": "..."
                val match = Regex("\"csv_url\"\\s*:\\s*\"([^\"]+)\"").find(configJson)
                if (match != null) {
                    targetUrl = match.groupValues[1]
                }
            } catch (e: Exception) {
                Log.w("FraudRepository", "Failed to fetch config, using default", e)
            }

            Log.d("FraudRepository", "Downloading DB from: $targetUrl")
            val csvContent = apiService.downloadDatabase(targetUrl)
            val lines = csvContent.lines()
            val sites = mutableListOf<FraudSite>()
            
            // CSV Parsing Logic ported from JS
            // Header starts at line 0, data starts from index 2?
            // JS: for (let i = 2; i < lines.length; i++)
            
            for (i in 2 until lines.size) {
                val line = lines[i].trim()
                if (line.isEmpty()) continue

                // Regex to split by comma, ignoring commas inside quotes
                // Kotlin Regex
                val parts = line.split(",(?=(?:(?:[^\"]*\"){2})*[^\"]*$)".toRegex())

                if (parts.size >= 2) {
                    val cleanParts = parts.map { it.trim().removeSurrounding("\"") }
                    
                    val name = cleanParts[0]
                    val rawUrl = cleanParts[1]
                    val count = cleanParts.getOrNull(2)?.toIntOrNull() ?: 0
                    val startDate = cleanParts.getOrNull(3) ?: ""
                    val endDate = cleanParts.getOrNull(4) ?: ""

                    if (rawUrl.isEmpty()) continue

                    // Normalize URL
                    var url = rawUrl.replace(Regex("^https?://"), "")
                    url = url.replace(Regex("/$"), "")

                    if (url.isNotEmpty()) {
                        sites.add(FraudSite(url, name, count, startDate, endDate))
                    }
                }
            }

            if (sites.isNotEmpty()) {
                fraudDao.deleteAll()
                fraudDao.insertAll(sites)
                Result.success(sites.size)
            } else {
                Result.failure(Exception("No data parsed"))
            }

        } catch (e: Exception) {
            Log.e("FraudRepository", "Error updating database", e)
            Result.failure(e)
        }
    }

    suspend fun checkUrl(urlToCheck: String): FraudSite? = withContext(Dispatchers.IO) {
        try {
            val urlObj = URL(if (urlToCheck.startsWith("http")) urlToCheck else "http://$urlToCheck")
            val hostname = urlObj.host

            // 1. Check exact match
            var result = fraudDao.getSite(hostname)
            if (result != null) return@withContext result

            // 2. Check w/o 'www.' if present
            if (hostname.startsWith("www.")) {
                result = fraudDao.getSite(hostname.substring(4))
                if (result != null) return@withContext result
            }

            // 3. Check w/ 'www.' if missing
            if (!hostname.startsWith("www.")) {
                result = fraudDao.getSite("www.$hostname")
                if (result != null) return@withContext result
            }

            return@withContext null
        } catch (e: Exception) {
            // Invalid URL
            return@withContext null
        }
    }
    
    suspend fun getDatabaseSize(): Int {
        return fraudDao.getCount()
    }
}
