package com.jackwu.nomorescamtw.repository

import android.util.Log
import com.jackwu.nomorescamtw.data.FraudDao
import com.jackwu.nomorescamtw.data.FraudSite
import com.jackwu.nomorescamtw.network.FraudApiService
import com.jackwu.nomorescamtw.network.GovApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.net.URL
import java.util.concurrent.TimeUnit

class FraudRepository(
    private val fraudDao: FraudDao,
    private val apiService: FraudApiService
) {

    suspend fun updateDatabase(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            // 1. Fetch Remote Config first to get the latest Gov API URL
            var govApiUrl = "https://data.gov.tw/api/v2/rest/dataset/" // Base URL default
            // Specific dataset ID "160055" is part of the path, but Retrofit defines base separately.
            // Let's assume the config returns the FULL URL or the ID. 
            // The User said: "github上放這個 https://data.gov.tw/api/v2/rest/dataset/160055"
            // Start with a clean string from config
            
            var targetDatasetUrl = "https://data.gov.tw/api/v2/rest/dataset/160055" 

            try {
                val configUrl = "https://cdn.jsdelivr.net/gh/asadman1523/NoMoreScamTW@main/server_config.json"
                val configJson = java.net.URL(configUrl).readText()
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
            
            // We need to handle the dynamic base URL since Retrofit requires a fixed base.
            // But we can use @Url with Retrofit to pass the full dynamic URL.
            // Update GovApiService to accept @Url
            
            // Re-create Retrofit client is expensive but safe for dynamic host
            // Extract Base URL safely, or just use the whole URL in get call
            // Since we need to use a dynamic URL, let's modify GovApiService usage to be dynamic or use Url parameter
            
            // Quick hack: Parse the base URL for Retrofit builder, ensuring it ends with /
            // URL: https://data.gov.tw/api/v2/rest/dataset/160055
            // Base: https://data.gov.tw/api/v2/rest/dataset/
            // Path: 160055
            
            // Since `targetDatasetUrl` can be anything, it's safer to not rely on splitting.
            // BUT, users asked to just "fetch this API".
            // Let's assume standard Gov API structure for now.
            
            // Simpler: Use OkHttp directly for this JSON fetch to avoid Retrofit dynamic URL complexity
            val client = OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build()
                
            val request = okhttp3.Request.Builder().url(targetDatasetUrl).build()
            val response = client.newCall(request).execute()
            val jsonStr = response.body?.string()
            
            if (!response.isSuccessful || jsonStr == null) {
                throw Exception("Gov API fetch failed: ${response.code}")
            }
            
            // Parse JSON manually or use regex to find resourceDownloadUrl
            // Doing a robust regex or simple search
            // JSON: ... "resourceDownloadUrl":"https://..." ...
            val urlRegex = "\"resourceDownloadUrl\"\\s*:\\s*\"([^\"]+)\"".toRegex()
            // There might be multiple distributions, usually we want the CSV one.
            // The user prompt implies taking the first one or the specific one.
            // Gov API JSON usually has CSV as first distribution.
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
                Result.success(entities.size)
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
