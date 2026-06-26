package com.example.restaurantordering.data.network

import okhttp3.Interceptor
import okhttp3.Response

class DynamicHostInterceptor : Interceptor {
    @Volatile var host: String? = null

    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()
        val currentHost = host
        if (currentHost != null) {
            val hostAndPort = currentHost.split(":")
            val hostName = hostAndPort[0]
            val port = if (hostAndPort.size > 1) hostAndPort[1].toIntOrNull() else null

            // Local addresses default to http, others (like Railway) default to https
            val isLocal = currentHost.contains("localhost") || 
                           currentHost.contains("10.0.2.2") || 
                           currentHost.contains("192.168.") ||
                           currentHost.contains("172.16.") ||
                           currentHost.contains("10.")
            val scheme = if (isLocal) "http" else "https"

            val newUrl = request.url.newBuilder()
                .scheme(scheme)
                .host(hostName)
                .apply {
                    if (port != null) {
                        port(port)
                    } else {
                        if (!isLocal) {
                            port(443)
                        } else {
                            port(3000)
                        }
                    }
                }
                .build()
            request = request.newBuilder().url(newUrl).build()
        }
        return chain.proceed(request)
    }
}
