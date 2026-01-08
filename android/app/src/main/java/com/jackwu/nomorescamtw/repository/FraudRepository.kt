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
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class FraudRepository(
    private val fraudDao: FraudDao,
    private val apiService: FraudApiService
) {

    suspend fun updateDatabase(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val entities = mutableListOf<FraudSite>()
            val visitedDomains = mutableSetOf<String>()
            
            // 1. Fetch Config Once
            var configMap: Map<*, *>? = null
            try {
                val configUrl = "https://cdn.jsdelivr.net/gh/asadman1523/NoMoreScamTW@main/server_config.json"
                val client = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).build()
                val request = okhttp3.Request.Builder().url(configUrl).build()
                val response = client.newCall(request).execute()
                val configJson = response.body()?.string()
                
                if (configJson != null) {
                    val gson = Gson()
                    configMap = gson.fromJson(configJson, Map::class.java) as Map<*, *>
                }
            } catch (e: Exception) {
                Log.w("FraudRepository", "Config fetch failed, using defaults", e)
            }

            // --- Source 1: Dataset 160055 (CSV) ---
            var count1 = 0
            try {
                count1 = fetchDataset160055(entities, visitedDomains, configMap)
            } catch (e: Exception) {
                Log.e("FraudRepository", "Failed to fetch dataset 160055", e)
            }

            // --- Source 2: Dataset 165027 (JSON) ---
            var count2 = 0
            try {
                count2 = fetchDataset165027(entities, visitedDomains, configMap)
            } catch (e: Exception) {
                Log.e("FraudRepository", "Failed to fetch dataset 165027", e)
            }

            if (entities.isNotEmpty()) {
                fraudDao.deleteAll()
                fraudDao.insertAll(entities)
                // Return total raw count as requested
                Result.success(count1 + count2)
            } else {
                Result.failure(Exception("No data parsed from any source"))
            }

        } catch (e: Exception) {
            Log.e("FraudRepository", "Error updating database", e)
            Result.failure(e)
        }
    }

    private suspend fun fetchDataset160055(entities: MutableList<FraudSite>, visitedDomains: MutableSet<String>, configMap: Map<*, *>?): Int {
        var targetDatasetUrl = "https://data.gov.tw/api/v2/rest/dataset/160055"
        var recordCount = 0
        
        // Try to override with server config
        try {
            val datasets = configMap?.get("datasets") as? List<Map<*, *>>
            datasets?.forEach { dataset ->
                if (dataset["id"] == "160055") {
                    val url = dataset["url"] as? String
                    if (!url.isNullOrEmpty()) {
                        targetDatasetUrl = url
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("FraudRepository", "Config parse failed 160055", e)
        }

        Log.i("FraudRepository", "Fetching metadata 160055: $targetDatasetUrl")
        val downloadUrl = fetchDownloadUrlFromMetadata(targetDatasetUrl) ?: run {
            Log.e("FraudRepository", "No download URL for 160055")
            return 0
        }

        Log.i("FraudRepository", "Downloading CSV 160055: $downloadUrl")
        val csvContent = apiService.downloadDatabase(downloadUrl)
        
        val lines = csvContent.lines()
        for (i in 2 until lines.size) {
            val line = lines[i].trim()
            if (line.isEmpty()) continue
            val parts = line.split(",(?=(?:(?:[^\"]*\"){2})*[^\"]*$)".toRegex())
            if (parts.size >= 2) {
                val cleanParts = parts.map { it.trim().removeSurrounding("\"") }
                val rawUrl = cleanParts[1]
                if (rawUrl.isEmpty()) continue
                
                var url = rawUrl.replace(Regex("^https?://"), "").replace(Regex("/$"), "")
                if (url.isNotEmpty()) {
                    recordCount++ // Count every valid URL found
                    if (!visitedDomains.contains(url)) {
                        val name = cleanParts[0]
                        val count = cleanParts.getOrNull(2)?.toIntOrNull() ?: 0
                        val startDate = cleanParts.getOrNull(3) ?: ""
                        val endDate = cleanParts.getOrNull(4) ?: ""
                        
                        entities.add(FraudSite(url, name, count, startDate, endDate))
                        visitedDomains.add(url)
                    }
                }
            }
        }
        return recordCount
    }

    private suspend fun fetchDataset165027(entities: MutableList<FraudSite>, visitedDomains: MutableSet<String>, configMap: Map<*, *>?): Int {
        var targetDatasetUrl = "https://data.gov.tw/api/v2/rest/dataset/165027"
        var recordCount = 0
        
        // Try to override with server config
        try {
             val datasets = configMap?.get("datasets") as? List<Map<*, *>>
             datasets?.forEach { dataset ->
                 if (dataset["id"] == "165027") {
                     val url = dataset["url"] as? String
                     if (!url.isNullOrEmpty()) {
                         targetDatasetUrl = url
                     }
                 }
             }
        } catch (e: Exception) {
            Log.w("FraudRepository", "Config parse failed 165027", e)
        }

        Log.i("FraudRepository", "Fetching metadata 165027: $targetDatasetUrl")

        val downloadUrl = fetchDownloadUrlFromMetadata(targetDatasetUrl, "JSON") ?: run {
            Log.e("FraudRepository", "No download URL for 165027")
            return 0
        }
        
        Log.i("FraudRepository", "Downloading JSON 165027: $downloadUrl")
        val jsonContent = apiService.downloadDatabase(downloadUrl)

        val gson = Gson()
        val listType = object : TypeToken<List<Map<String, String>>>() {}.type
        val rawList: List<Map<String, String>> = gson.fromJson(jsonContent, listType)

        for (item in rawList) {
            val domain = item["網域名稱"] ?: ""
            if (domain.isNotEmpty()) {
                recordCount++ // Count every valid domain found
                if (!visitedDomains.contains(domain)) {
                    // Dataset 165027 fields are different, we adapt them to match FraudSite
                    val name = "TWNIC Suspicious Domain" 
                    
                    entities.add(FraudSite(domain, name, 0, item["詐騙網站創建日期"] ?: "", ""))
                    visitedDomains.add(domain)
                }
            }
        }
        return recordCount
    }

    private fun fetchDownloadUrlFromMetadata(metadataUrl: String, format: String = ""): String? {
        try {
            val client = OkHttpClient.Builder().connectTimeout(30, TimeUnit.SECONDS).build()
            val request = okhttp3.Request.Builder().url(metadataUrl).build()
            val response = client.newCall(request).execute()
            val jsonStr = response.body()?.string() ?: return null
            if (!response.isSuccessful) return null

            // Simple regex to find resourceDownloadUrl. 
            // If format is specified, we might want to be more specific, but for now simplistic approach:
            // The 165027 has both XML and JSON. We want JSON.
            // The previous regex just took the first one.
            
            if (format == "JSON") {
                // Look for JSON format then the url
                // This is a bit hacky with regex on full json. 
                // Better to iterate over "distribution" array if we parsed it properly.
                // But let's try a regex that looks for "resourceFormat":"JSON" and capturing the URL in the same block?
                // Or just use Gson since we added it.
                val gson = Gson()
                val map = gson.fromJson(jsonStr, Map::class.java) as Map<*, *>
                val result = map["result"] as? Map<*, *>
                val distributions = result?.get("distribution") as? List<Map<*, *>>
                
                if (distributions != null) {
                    for (dist in distributions) {
                         if (dist["resourceFormat"] == "JSON") {
                             return (dist["resourceDownloadUrl"] as? String)?.replace("\\/", "/")
                         }
                    }
                }
            }

            // Fallback or default behavior (first link)
            val urlRegex = "\"resourceDownloadUrl\"\\s*:\\s*\"([^\"]+)\"".toRegex()
            return urlRegex.find(jsonStr)?.groupValues?.get(1)?.replace("\\/", "/")
            
        } catch (e: Exception) {
            Log.e("FraudRepository", "Metadata fetch error", e)
            return null
        }
    }

    suspend fun checkUrl(urlToCheck: String): FraudSite? = withContext(Dispatchers.IO) {
        try {

            // Clean URL: remove protocol and trailing slash
            val cleanFullUrl = urlToCheck.replace(Regex("^https?://"), "").replace(Regex("/$"), "")

            val urlObj = URL(if (urlToCheck.startsWith("http")) urlToCheck else "http://$urlToCheck")
            val hostname = urlObj.host

            // 1. Check Full Clean URL (includes path)
            var result = fraudDao.getSite(cleanFullUrl)
            if (result != null) return@withContext result

            // 2. Check exact match (hostname)
            if (cleanFullUrl != hostname) {
                result = fraudDao.getSite(hostname)
                if (result != null) return@withContext result
            }

            // 3. Check w/o 'www.' if present
            if (hostname.startsWith("www.")) {
                result = fraudDao.getSite(hostname.substring(4))
                if (result != null) return@withContext result
            }

            // 4. Check w/ 'www.' if missing
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
