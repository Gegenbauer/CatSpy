package me.gegenbauer.catspy.common

import java.io.InputStream
import java.net.URL

object Resources {
    fun loadResourceAsStream(resourcePath: String): InputStream {
        val contextClassLoader = Thread.currentThread().contextClassLoader
        val resource = contextClassLoader.getResourceAsStream(resourcePath)
        return requireNotNull(resource) { "Resource $resourcePath not found" }
    }

    fun getResource(relativePath: String): URL {
        val classLoader = Thread.currentThread().contextClassLoader
        val resource = classLoader.getResource(relativePath)
        return resource ?: throw IllegalArgumentException("Resource not found")
    }
}