package me.gegenbauer.catspy.network

import okhttp3.MediaType
import okhttp3.ResponseBody
import okio.*
import java.io.File

fun interface DownloadListener {
    fun onDownloadStart() {}

    fun onProgressChanged(bytesRead: Long, contentLength: Long)

    fun onDownloadComplete(file: File) {}

    fun onDownloadCanceled() {}

    fun onDownloadFailed(e: Throwable) {}
}

class ProgressResponseBody(
    private val responseBody: ResponseBody,
    private val downloadListener: DownloadListener
) : ResponseBody() {

    private var bufferedSource: BufferedSource? = null

    override fun contentType(): MediaType? {
        return responseBody.contentType()
    }

    override fun contentLength(): Long {
        return responseBody.contentLength()
    }

    override fun source(): BufferedSource {
        if (bufferedSource == null) {
            bufferedSource = source(responseBody.source()).buffer()
        }
        return bufferedSource!!
    }

    private fun source(source: Source): Source {
        return object : ForwardingSource(source) {
            var totalBytesRead = 0L

            override fun read(sink: Buffer, byteCount: Long): Long {
                val bytesRead = super.read(sink, byteCount)
                totalBytesRead += if (bytesRead != -1L) bytesRead else 0
                downloadListener.onProgressChanged(totalBytesRead, responseBody.contentLength())
                return bytesRead
            }
        }
    }
}