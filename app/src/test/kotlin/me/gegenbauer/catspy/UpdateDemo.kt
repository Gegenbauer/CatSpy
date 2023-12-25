package me.gegenbauer.catspy

import kotlinx.coroutines.runBlocking
import me.gegenbauer.catspy.network.DownloadListener
import me.gegenbauer.catspy.network.update.GithubUpdateServiceFactory
import me.gegenbauer.catspy.network.update.data.Release
import java.io.File

fun main() {
    val updateService = GithubUpdateServiceFactory.create("skylot", "jadx")
    runBlocking {
        val latestRelease = updateService.getLatestRelease()
        println(latestRelease)
        if (latestRelease.isFailure) {
            println("Failed to get latest release")
            return@runBlocking
        }
        val updateAvailable = updateService.checkForUpdate(latestRelease.getOrThrow(), Release("1.4.6"))
        println("Update available: $updateAvailable")
        val asset = latestRelease.getOrThrow().assets.firstOrNull()
        asset?.let {
            println("Downloading asset: $it")
            updateService.downloadAsset(it, "/home/yingbin/build.zip", object : DownloadListener {

                override fun onDownloadStart() {
                    println("Download started")
                }

                override fun onProgressChanged(bytesRead: Long, contentLength: Long) {
                    println("Download progress: $bytesRead/$contentLength")
                }

                override fun onDownloadComplete(file: File) {
                    println("Download completed")
                }

                override fun onDownloadCanceled() {
                    println("Download canceled")
                }
            })
        }
    }
}