package com.runanywhere.startup_hackathon20.youtube

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit service interface for YouTube Data API v3
 */
interface YouTubeApiService {

    @GET("search")
    suspend fun searchVideos(
        @Query("part") part: String = "snippet",
        @Query("q") query: String,
        @Query("type") type: String = "video",
        @Query("maxResults") maxResults: Int = 10,
        @Query("order") order: String = "relevance",
        @Query("key") apiKey: String
    ): Response<YouTubeSearchResponse>

    @GET("search")
    suspend fun searchChannels(
        @Query("part") part: String = "snippet",
        @Query("q") query: String,
        @Query("type") type: String = "channel",
        @Query("maxResults") maxResults: Int = 5,
        @Query("order") order: String = "relevance",
        @Query("key") apiKey: String
    ): Response<YouTubeSearchResponse>

    @GET("videos")
    suspend fun getVideoDetails(
        @Query("part") part: String = "statistics,contentDetails",
        @Query("id") videoIds: String,
        @Query("key") apiKey: String
    ): Response<YouTubeVideoDetailsResponse>

    @GET("channels")
    suspend fun getChannelDetails(
        @Query("part") part: String = "statistics",
        @Query("id") channelIds: String,
        @Query("key") apiKey: String
    ): Response<YouTubeChannelDetailsResponse>
}
