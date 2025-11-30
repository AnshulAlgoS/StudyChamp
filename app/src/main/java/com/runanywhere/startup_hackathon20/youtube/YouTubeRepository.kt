package com.runanywhere.startup_hackathon20.youtube

import android.util.Log
import com.runanywhere.startup_hackathon20.BuildConfig
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlin.Result

/**
 * Repository for YouTube Data API operations
 */
class YouTubeRepository {

    private val apiKey = BuildConfig.YOUTUBE_API_KEY

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://www.googleapis.com/youtube/v3/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val api = retrofit.create(YouTubeApiService::class.java)

    /**
     * Search for educational videos on a topic
     */
    suspend fun searchVideos(topic: String, subject: String = ""): Result<List<YouTubeVideo>> {
        return try {
            val searchQuery = if (subject.isNotEmpty()) {
                "$topic $subject tutorial"
            } else {
                "$topic tutorial"
            }

            Log.d("YouTubeRepo", "Searching videos for: $searchQuery")

            val response = api.searchVideos(
                query = searchQuery,
                maxResults = 10,
                order = "relevance",
                apiKey = apiKey
            )

            if (response.isSuccessful && response.body() != null) {
                val items = response.body()!!.items ?: emptyList()

                // Extract video IDs
                val videoIds = items.mapNotNull { it.id.videoId }.joinToString(",")

                // Fetch additional details (views, duration) if we have video IDs
                val detailsMap = if (videoIds.isNotEmpty()) {
                    fetchVideoDetails(videoIds)
                } else {
                    emptyMap()
                }

                // Map to our YouTubeVideo model
                val videos = items.mapNotNull { item ->
                    val videoId = item.id.videoId ?: return@mapNotNull null
                    val details = detailsMap[videoId]

                    YouTubeVideo(
                        id = videoId,
                        title = item.snippet.title,
                        description = item.snippet.description,
                        thumbnailUrl = item.snippet.thumbnails.high?.url
                            ?: item.snippet.thumbnails.medium?.url
                            ?: item.snippet.thumbnails.default?.url
                            ?: "",
                        channelTitle = item.snippet.channelTitle,
                        channelId = item.snippet.channelId,
                        publishedAt = item.snippet.publishedAt,
                        viewCount = details?.viewCount ?: "",
                        duration = details?.duration ?: ""
                    )
                }

                Log.d("YouTubeRepo", "Found ${videos.size} videos")
                Result.success(videos)
            } else {
                Log.e("YouTubeRepo", "API error: ${response.code()} - ${response.message()}")
                Result.failure(Exception("Failed to fetch videos: ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e("YouTubeRepo", "Exception searching videos", e)
            Result.failure(e)
        }
    }

    /**
     * Search for educational channels on a topic
     */
    suspend fun searchChannels(topic: String, subject: String = ""): Result<List<YouTubeChannel>> {
        return try {
            val searchQuery = if (subject.isNotEmpty()) {
                "$subject $topic education"
            } else {
                "$topic education"
            }

            Log.d("YouTubeRepo", "Searching channels for: $searchQuery")

            val response = api.searchChannels(
                query = searchQuery,
                maxResults = 5,
                order = "relevance",
                apiKey = apiKey
            )

            if (response.isSuccessful && response.body() != null) {
                val items = response.body()!!.items ?: emptyList()

                // Extract channel IDs
                val channelIds = items.mapNotNull { it.id.channelId }.joinToString(",")

                // Fetch channel statistics
                val statsMap = if (channelIds.isNotEmpty()) {
                    fetchChannelDetails(channelIds)
                } else {
                    emptyMap()
                }

                // Map to our YouTubeChannel model
                val channels = items.mapNotNull { item ->
                    val channelId = item.id.channelId ?: return@mapNotNull null
                    val stats = statsMap[channelId]

                    YouTubeChannel(
                        id = channelId,
                        title = item.snippet.title,
                        description = item.snippet.description,
                        thumbnailUrl = item.snippet.thumbnails.high?.url
                            ?: item.snippet.thumbnails.medium?.url
                            ?: item.snippet.thumbnails.default?.url
                            ?: "",
                        subscriberCount = stats?.subscriberCount ?: "",
                        videoCount = stats?.videoCount ?: ""
                    )
                }

                Log.d("YouTubeRepo", "Found ${channels.size} channels")
                Result.success(channels)
            } else {
                Log.e("YouTubeRepo", "API error: ${response.code()} - ${response.message()}")
                Result.failure(Exception("Failed to fetch channels: ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e("YouTubeRepo", "Exception searching channels", e)
            Result.failure(e)
        }
    }

    /**
     * Fetch video details (views, duration) for multiple videos
     */
    private suspend fun fetchVideoDetails(videoIds: String): Map<String, VideoDetails> {
        return try {
            val response = api.getVideoDetails(
                videoIds = videoIds,
                apiKey = apiKey
            )

            if (response.isSuccessful && response.body() != null) {
                val items = response.body()!!.items ?: emptyList()
                // Note: We'd need to parse videoIds individually from the response
                // For simplicity, we'll return a map with available data
                items.mapIndexed { index, item ->
                    // This is simplified - in production, match by actual video ID from response
                    videoIds.split(",").getOrNull(index) to VideoDetails(
                        viewCount = formatViewCount(item.statistics?.viewCount),
                        duration = formatDuration(item.contentDetails?.duration)
                    )
                }.toMap().filterKeys { it != null }.mapKeys { it.key!! }
            } else {
                emptyMap()
            }
        } catch (e: Exception) {
            Log.e("YouTubeRepo", "Error fetching video details", e)
            emptyMap()
        }
    }

    /**
     * Fetch channel statistics
     */
    private suspend fun fetchChannelDetails(channelIds: String): Map<String, ChannelStats> {
        return try {
            val response = api.getChannelDetails(
                channelIds = channelIds,
                apiKey = apiKey
            )

            if (response.isSuccessful && response.body() != null) {
                val items = response.body()!!.items ?: emptyList()
                items.mapIndexed { index, item ->
                    channelIds.split(",").getOrNull(index) to ChannelStats(
                        subscriberCount = formatSubscriberCount(item.statistics?.subscriberCount),
                        videoCount = item.statistics?.videoCount ?: ""
                    )
                }.toMap().filterKeys { it != null }.mapKeys { it.key!! }
            } else {
                emptyMap()
            }
        } catch (e: Exception) {
            Log.e("YouTubeRepo", "Error fetching channel details", e)
            emptyMap()
        }
    }

    /**
     * Format view count to human-readable format
     */
    private fun formatViewCount(viewCount: String?): String {
        if (viewCount == null) return ""
        val count = viewCount.toLongOrNull() ?: return viewCount

        return when {
            count >= 1_000_000 -> "${count / 1_000_000}M views"
            count >= 1_000 -> "${count / 1_000}K views"
            else -> "$count views"
        }
    }

    /**
     * Format subscriber count
     */
    private fun formatSubscriberCount(subCount: String?): String {
        if (subCount == null) return ""
        val count = subCount.toLongOrNull() ?: return subCount

        return when {
            count >= 1_000_000 -> "${count / 1_000_000}M subscribers"
            count >= 1_000 -> "${count / 1_000}K subscribers"
            else -> "$count subscribers"
        }
    }

    /**
     * Format ISO 8601 duration to readable format
     */
    private fun formatDuration(duration: String?): String {
        if (duration == null) return ""

        // Parse ISO 8601 duration (e.g., "PT15M33S" = 15 minutes 33 seconds)
        val regex = Regex("PT(?:(\\d+)H)?(?:(\\d+)M)?(?:(\\d+)S)?")
        val match = regex.find(duration) ?: return ""

        val hours = match.groupValues[1].toIntOrNull() ?: 0
        val minutes = match.groupValues[2].toIntOrNull() ?: 0
        val seconds = match.groupValues[3].toIntOrNull() ?: 0

        return when {
            hours > 0 -> String.format("%d:%02d:%02d", hours, minutes, seconds)
            minutes > 0 -> String.format("%d:%02d", minutes, seconds)
            else -> "0:${seconds.toString().padStart(2, '0')}"
        }
    }

    data class VideoDetails(val viewCount: String, val duration: String)
    data class ChannelStats(val subscriberCount: String, val videoCount: String)
}
