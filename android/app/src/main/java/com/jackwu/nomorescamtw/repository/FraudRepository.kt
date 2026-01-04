package com.jackwu.nomorescamtw.repository

import android.util.Log
import com.jackwu.nomorescamtw.data.FraudDao
import com.jackwu.nomorescamtw.data.FraudSite
import com.jackwu.nomorescamtw.network.FraudApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.net.URL
import java.util.concurrent.TimeUnit

class FraudRepository(
    private val fraudDao: FraudDao,
    private val apiService: FraudApiService
) {

    suspend fun updateDatabase(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            
            var targetDatasetUrl = "https://data.gov.tw/api/v2/rest/dataset/160055" 

            try {
                val configUrl = "https://cdn.jsdelivr.net/gh/asadman1523/NoMoreScamTW@main/server_config.json"
                val configJson = URL(configUrl).readText()
                val regex = "\"fraud_api_url\"\\s*:\\s*\"([^\"]+)\"".toRegex()
                val matchResult = regex.find(configJson)
                val remoteApiUrl = matchResult?.groupValues?.get(1)
                
                if (remoteApiUrl != null) {
                    targetDatasetUrl = remoteApiUrl
                }
            } catch (e: Exception) {
                Log.w("FraudRepository", "Config fetch failed, using default Gov URL", e)
            }

            Log.i("FraudRepository", "Fetching metadata from: $targetDatasetUrl")

            val client = OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build()
                
            val request = okhttp3.Request.Builder().url(targetDatasetUrl).build()
            val response = client.newCall(request).execute()
            val jsonStr = response.body()?.string()

            if (!response.isSuccessful || jsonStr == null) {
                throw Exception("Gov API fetch failed: ${response.code()}")
            }

            val urlRegex = "\"resourceDownloadUrl\"\\s*:\\s*\"([^\"]+)\"".toRegex()

            val fileMatch = urlRegex.find(jsonStr)
            val downloadUrl = fileMatch?.groupValues?.get(1)?.replace("\\/", "/") // Unescape slashes
            
            if (downloadUrl.isNullOrEmpty()) {
                throw Exception("No download link found in Gov API response")
            }

            Log.i("FraudRepository", "Downloading CSV from: $downloadUrl")

            // 2. Download CSV Content
            val csvContent = apiService.downloadDatabase(downloadUrl)
            
            // 3. Parse CSV
            val lines = csvContent.lines()
            val entities = mutableListOf<FraudSite>()

            for (i in 2 until lines.size) {
                val line = lines[i].trim()
                if (line.isEmpty()) continue

                // Regex to split by comma, ignoring commas inside quotes
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
                        entities.add(FraudSite(url, name, count, startDate, endDate))
                    }
                }
            }

            if (entities.isNotEmpty()) {
                fraudDao.deleteAll()
                fraudDao.insertAll(entities)
                // Returning total rows processed (including duplicates) as requested by user
                Result.success(lines.size - 2) 
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
