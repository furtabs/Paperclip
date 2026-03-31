package com.furtabs.paperclip.data.utils

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET

data class GithubRelease(
    @SerializedName("tag_name") val tagName: String,
    @SerializedName("html_url") val htmlUrl: String,
    @SerializedName("body") val body: String,
    @SerializedName("published_at") val releaseDate: String
)

interface GithubApi {
    @GET("repos/furtabs/Paperclip/releases/latest")
    suspend fun getLatestRelease(): GithubRelease
    @GET("repos/furtabs/Paperclip/releases")
    suspend fun getReleases(): List<GithubRelease>
}