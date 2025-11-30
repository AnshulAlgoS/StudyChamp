package com.runanywhere.startup_hackathon20.youtube

import com.google.gson.annotations.SerializedName

/**
 * YouTube Video Search Result
 */
data class YouTubeVideo(
    val id: String,
    val title: String,
    val description: String,
    val thumbnailUrl: String,
    val channelTitle: String,
    val channelId: String,
    val publishedAt: String,
    val viewCount: String = "",
    val duration: String = ""
)

/**
 * YouTube Channel Result
 */
data class YouTubeChannel(
    val id: String,
    val title: String,
    val description: String,
    val thumbnailUrl: String,
    val subscriberCount: String = "",
    val videoCount: String = ""
)

// API Response Models (matching YouTube Data API v3 structure)

data class YouTubeSearchResponse(
    val items: List<YouTubeSearchItem>?
)

data class YouTubeSearchItem(
    val id: YouTubeSearchId,
    val snippet: YouTubeSnippet
)

data class YouTubeSearchId(
    val kind: String,
    val videoId: String?,
    val channelId: String?
)

data class YouTubeSnippet(
    val publishedAt: String,
    val channelId: String,
    val title: String,
    val description: String,
    val thumbnails: YouTubeThumbnails,
    val channelTitle: String
)

data class YouTubeThumbnails(
    val default: YouTubeThumbnail?,
    val medium: YouTubeThumbnail?,
    val high: YouTubeThumbnail?
)

data class YouTubeThumbnail(
    val url: String,
    val width: Int?,
    val height: Int?
)

// Video details response
data class YouTubeVideoDetailsResponse(
    val items: List<YouTubeVideoDetailsItem>?
)

data class YouTubeVideoDetailsItem(
    val statistics: YouTubeStatistics?,
    val contentDetails: YouTubeContentDetails?
)

data class YouTubeStatistics(
    val viewCount: String?,
    val likeCount: String?,
    val commentCount: String?
)

data class YouTubeContentDetails(
    val duration: String?
)

// Channel details response
data class YouTubeChannelDetailsResponse(
    val items: List<YouTubeChannelDetailsItem>?
)

data class YouTubeChannelDetailsItem(
    val statistics: YouTubeChannelStatistics?
)

data class YouTubeChannelStatistics(
    val subscriberCount: String?,
    val videoCount: String?,
    val viewCount: String?
)
