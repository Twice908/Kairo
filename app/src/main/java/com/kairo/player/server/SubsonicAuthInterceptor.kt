package com.kairo.player.server

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.security.MessageDigest
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubsonicAuthInterceptor @Inject constructor(
    private val config: ServerConfig,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val server = config.serverUrl.toHttpUrlOrNull()
            ?: throw IOException("Server URL is not configured")

        val salt = randomSalt()
        val token = md5(config.password + salt)

        val url = request.url.newBuilder()
            .scheme(server.scheme)
            .host(server.host)
            .port(server.port)
            .addQueryParameter("u", config.username)
            .addQueryParameter("t", token)
            .addQueryParameter("s", salt)
            .addQueryParameter("v", "1.16.1")
            .addQueryParameter("c", "Kairo")
            .addQueryParameter("f", "json")
            .build()

        return chain.proceed(request.newBuilder().url(url).build())
    }

    private fun md5(input: String): String =
        MessageDigest.getInstance("MD5")
            .digest(input.toByteArray())
            .joinToString("") { "%02x".format(it) }

    private fun randomSalt(): String {
        val bytes = ByteArray(8)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }
}