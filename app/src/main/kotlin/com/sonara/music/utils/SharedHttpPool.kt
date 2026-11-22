package com.sonara.music.utils

import com.sonara.innertube.YouTube
import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import java.net.Proxy
import java.util.concurrent.TimeUnit

object SharedHttpPool {
    private val sharedConnectionPool = ConnectionPool(16, 5, TimeUnit.MINUTES)
    private val sharedDispatcher = Dispatcher()

    val baseClient: OkHttpClient =
        OkHttpClient
            .Builder()
            .connectionPool(sharedConnectionPool)
            .dispatcher(sharedDispatcher)
            .build()

    fun newClientBuilder(): OkHttpClient.Builder = baseClient.newBuilder()

    @Volatile
    private var cachedProxy: Proxy? = null

    @Volatile
    private var cachedProxyAuth: String? = null

    @Volatile
    private var cachedProxiedClient: OkHttpClient? = null

    @Synchronized
    fun proxiedClient(): OkHttpClient {
        val currentProxy = YouTube.proxy
        val currentAuth = YouTube.proxyAuth
        val existing = cachedProxiedClient
        if (existing != null && cachedProxy == currentProxy && cachedProxyAuth == currentAuth) {
            return existing
        }
        val created =
            baseClient
                .newBuilder()
                .proxy(currentProxy)
                .proxyAuthenticator { _, response ->
                    YouTube.proxyAuth?.let { auth ->
                        response.request
                            .newBuilder()
                            .header("Proxy-Authorization", auth)
                            .build()
                    } ?: response.request
                }.build()
        cachedProxy = currentProxy
        cachedProxyAuth = currentAuth
        cachedProxiedClient = created
        return created
    }
}
