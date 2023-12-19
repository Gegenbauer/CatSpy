package me.gegenbauer.catspy.network.update.data

import com.google.gson.annotations.SerializedName

data class ErrorMessage(
    val message: String,
    @SerializedName("documentation_url") val documentUrl: String
)