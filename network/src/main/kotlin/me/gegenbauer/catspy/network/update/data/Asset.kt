package me.gegenbauer.catspy.network.update.data

import com.google.gson.annotations.SerializedName

data class Asset(
    val id: Int,
    val name: String,
    val size: Long,
    @SerializedName("download_count") val downloadCount: Int,
    @SerializedName("browser_download_url") val downloadUrl: String,
    @SerializedName("created_at") val createdAt: String,
)