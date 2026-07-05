package com.example.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

class ChannelRepository(private val context: Context) {
    private val channelDao = AppDatabase.getDatabase(context).channelDao()

    val allChannels: Flow<List<ChannelEntity>> = channelDao.getAllChannels()
    val favorites: Flow<List<ChannelEntity>> = channelDao.getFavorites()

    suspend fun initializeIfNeeded() {
        withContext(Dispatchers.IO) {
            val count = channelDao.getAllChannels().first().size
            if (count == 0) {
                loadChannelsFromAssets()
            }
        }
    }

    private suspend fun loadChannelsFromAssets() {
        try {
            val jsonString = context.assets.open("channels.json").bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(jsonString)
            val channels = mutableListOf<ChannelEntity>()
            for (i in 0 until jsonArray.length()) {
                val countryObj = jsonArray.getJSONObject(i)
                val countryName = countryObj.getString("country")
                val channelsArray = countryObj.getJSONArray("channels")
                for (j in 0 until channelsArray.length()) {
                    val chObj = channelsArray.getJSONObject(j)
                    val name = chObj.getString("name")
                    val url = chObj.getString("url")
                    val logo = chObj.optString("logo", "")
                    val category = chObj.optString("category", "General")
                    val id = "asset_${countryName.lowercase().filter { it.isLetterOrDigit() }}_${name.lowercase().filter { it.isLetterOrDigit() }}"
                    channels.add(
                        ChannelEntity(
                            id = id,
                            name = name,
                            logo = logo,
                            country = countryName,
                            category = category,
                            streamUrl = url,
                            isFavorite = false,
                            isCustom = false
                        )
                    )
                }
            }
            channelDao.insertChannels(channels)
            Log.d("ChannelRepository", "Loaded ${channels.size} channels from assets.")
        } catch (e: Exception) {
            Log.e("ChannelRepository", "Error loading default channels: ${e.message}", e)
        }
    }

    suspend fun toggleFavorite(channelId: String, isFavorite: Boolean) {
        channelDao.updateFavoriteStatus(channelId, isFavorite)
    }

    suspend fun clearCache() {
        channelDao.clearAll()
        loadChannelsFromAssets()
    }

    suspend fun addChannel(channel: ChannelEntity) {
        channelDao.insertChannel(channel)
    }

    suspend fun deleteChannel(channel: ChannelEntity) {
        channelDao.deleteChannel(channel)
    }

    suspend fun importFromUrl(urlString: String, format: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.requestMethod = "GET"
            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext Result.failure(Exception("HTTP Error: ${connection.responseCode}"))
            }

            val reader = BufferedReader(InputStreamReader(connection.inputStream))
            val content = reader.use { it.readText() }

            if (format.lowercase() == "m3u" || content.trim().startsWith("#EXTM3U")) {
                importM3UContent(content)
            } else {
                importJSONContent(content)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importM3UContent(m3uContent: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val lines = m3uContent.split("\n")
            val channels = mutableListOf<ChannelEntity>()
            var currentChannelName = ""
            var currentLogo = ""
            var currentCountry = "Imported"
            var currentCategory = "General"

            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.isEmpty()) continue

                if (trimmed.startsWith("#EXTINF:")) {
                    // Extract channel info
                    // Parse tvg-logo
                    currentLogo = extractAttribute(trimmed, "tvg-logo") ?: extractAttribute(trimmed, "logo") ?: ""
                    
                    // Parse group-title (often country/category)
                    currentCountry = extractAttribute(trimmed, "group-title") ?: extractAttribute(trimmed, "country") ?: "Imported"
                    currentCategory = extractAttribute(trimmed, "category") ?: "General"
                    
                    // The channel name is at the end of the EXTINF line, after the last comma
                    val commaIndex = trimmed.lastIndexOf(",")
                    currentChannelName = if (commaIndex != -1 && commaIndex < trimmed.length - 1) {
                        trimmed.substring(commaIndex + 1).trim()
                    } else {
                        "Unknown IPTV Channel"
                    }
                } else if (!trimmed.startsWith("#")) {
                    // This line contains the stream URL
                    if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                        val id = "imported_" + UUID.randomUUID().toString().take(8)
                        val name = currentChannelName.ifEmpty { "Channel " + id.takeLast(4) }
                        
                        channels.add(
                            ChannelEntity(
                                id = id,
                                name = name,
                                logo = currentLogo,
                                country = currentCountry,
                                category = currentCategory,
                                streamUrl = trimmed,
                                isFavorite = false,
                                isCustom = true
                            )
                        )
                        // Reset transient attributes
                        currentChannelName = ""
                        currentLogo = ""
                        currentCountry = "Imported"
                        currentCategory = "General"
                    }
                }
            }

            if (channels.isNotEmpty()) {
                channelDao.insertChannels(channels)
                Result.success(channels.size)
            } else {
                Result.failure(Exception("No valid M3U8 links found in the input"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun extractAttribute(line: String, attributeName: String): String? {
        val searchKey = "$attributeName=\""
        val startIndex = line.indexOf(searchKey)
        if (startIndex != -1) {
            val valueStart = startIndex + searchKey.length
            val endIndex = line.indexOf("\"", valueStart)
            if (endIndex != -1) {
                return line.substring(valueStart, endIndex)
            }
        }
        return null
    }

    suspend fun importJSONContent(jsonContent: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val jsonArray = JSONArray(jsonContent)
            val channels = mutableListOf<ChannelEntity>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val id = obj.optString("id", "imported_" + UUID.randomUUID().toString().take(8))
                channels.add(
                    ChannelEntity(
                        id = id,
                        name = obj.getString("name"),
                        logo = obj.optString("logo", ""),
                        country = obj.optString("country", "Imported"),
                        category = obj.optString("category", "General"),
                        streamUrl = obj.getString("stream_url"),
                        isFavorite = false,
                        isCustom = true
                    )
                )
            }
            if (channels.isNotEmpty()) {
                channelDao.insertChannels(channels)
                Result.success(channels.size)
            } else {
                Result.failure(Exception("No valid channels found in the JSON"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
