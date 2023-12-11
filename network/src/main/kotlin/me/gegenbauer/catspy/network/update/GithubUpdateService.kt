package me.gegenbauer.catspy.network.update

import kotlinx.coroutines.*
import me.gegenbauer.catspy.concurrency.GIO
import me.gegenbauer.catspy.file.gson
import me.gegenbauer.catspy.network.DownloadListener
import me.gegenbauer.catspy.network.NetworkClient
import me.gegenbauer.catspy.network.ProgressResponseBody
import me.gegenbauer.catspy.network.update.data.Asset
import me.gegenbauer.catspy.network.update.data.Release
import okhttp3.Call
import okhttp3.Request
import okhttp3.internal.http2.StreamResetException
import java.io.File
import java.io.IOException
import java.net.SocketException

interface GithubUpdateService {
    val user: String

    val repo: String

    suspend fun getLatestRelease(): Release

    suspend fun checkForUpdate(latestRelease: Release, currentRelease: Release): Boolean

    suspend fun downloadAsset(asset: Asset, downloadPath: String, downloadListener: DownloadListener)

    fun cancelDownload()
}

object GithubUpdateServiceFactory {
    @JvmStatic
    fun create(user: String, repo: String): GithubUpdateService {
        return GithubUpdateServiceImpl(user, repo)
    }
}

class GithubUpdateServiceImpl(override val user: String, override val repo: String) : GithubUpdateService {

    private val url = LATEST_RELEASES_URL.format(user, repo)
    private var currentDownloadCall: Call? = null
    private var downloadJob: Job? = null

    override suspend fun getLatestRelease(): Release {
        return withContext(Dispatchers.GIO) {
            val request = Request.Builder()
                .url(url)
                .build()
            val response = NetworkClient.client.newCall(request).execute()
            if (response.code != 200) {
                response.close()
                throw Exception("Failed to get latest release $url, code: ${response.code}")
            }
            val body = response.body?.string() ?: throw Exception("Failed to get latest release, body is null")
            parseRelease(body)
        }
    }

    override suspend fun checkForUpdate(latestRelease: Release, currentRelease: Release): Boolean {
        return latestRelease > currentRelease
    }

    override suspend fun downloadAsset(asset: Asset, downloadPath: String, downloadListener: DownloadListener) {
        withContext(Dispatchers.GIO) {
            downloadJob = coroutineContext.job
            val file = File(downloadPath)
            coroutineContext.job.invokeOnCompletion {
                if (it is CancellationException) {
                    downloadListener.onDownloadCanceled()
                } else if (it != null) {
                    downloadListener.onDownloadFailed(it)
                } else {
                    downloadListener.onDownloadComplete(file)
                }
            }
            runCatching {
                val request = Request.Builder()
                    .url(asset.downloadUrl)
                    .build()
                currentDownloadCall = NetworkClient.client.newCall(request)
                ensureActive()
                val response = currentDownloadCall!!.execute()
                if (response.code != 200) {
                    response.close()
                    throw IOException("Failed to download latest release, code: ${response.code}")
                }
                downloadListener.onDownloadStart()
                val body = response.body ?: throw Exception("Failed to download latest release, body is null")
                val progressBody = ProgressResponseBody(body, downloadListener)
                val assetStream = progressBody.byteStream()
                file.outputStream().use { outputStream ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var bytes = assetStream.read(buffer)
                    while (bytes != -1) {
                        ensureActive() // Check if the job is still active
                        outputStream.write(buffer, 0, bytes)
                        bytes = assetStream.read(buffer)
                    }
                }
            }.onFailure {
                if (it is SocketException || it is StreamResetException) {
                    throw CancellationException("Download canceled")
                } else {
                    throw it
                }
            }
        }
    }

    override fun cancelDownload() {
        downloadJob?.cancel()
        currentDownloadCall?.cancel()
    }

    private fun parseRelease(json: String): Release {
        return gson.fromJson(json, Release::class.java)
    }

    companion object {
        private const val LATEST_RELEASES_URL = "https://api.github.com/repos/%s/%s/releases/latest"
    }
}