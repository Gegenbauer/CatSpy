package me.gegenbauer.catspy.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.net.Proxy
import java.util.concurrent.TimeUnit

object NetworkClient {

    private val defaultClientBuilder = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .proxy(Proxy(Proxy.Type.HTTP, java.net.InetSocketAddress("127.0.0.1",7890)))

    var debug: Boolean = false
        set(value) {
            field = value
            client = defaultClientBuilder.addInterceptor(HttpLoggingInterceptor().apply {
                //level = if (value) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
            }).build()
        }

    var client: OkHttpClient = defaultClientBuilder.addInterceptor(HttpLoggingInterceptor().apply {
        //level = HttpLoggingInterceptor.Level.BODY
    }).build()
}