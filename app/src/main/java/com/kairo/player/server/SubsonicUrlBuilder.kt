package com.kairo.player.server

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.security.MessageDigest
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubsonicUrlBuilder @Inject constructor(
    private val config: ServerConfig,
) {
    // Fixed per app run so URLs stay identical and cache keys stay stable.
    private val salt: String = ByteArray(8).also { SecureRandom().nextBytes(it) }
        .joinToString("") { "%02x".format(it) }

    fun streamUrl(id: String): String? = build("rest/stream") {
        addQueryParameter("id", id)
        addQueryParameter("format", "raw") // original file, no transcoding
    }

    fun coverArtUrl(id: String, size: Int = 600): String? = build("rest/getCoverArt") {
        addQueryParameter("id", id)
        addQueryParameter("size", size.toString())
    }

    private fun build(path: String, extra: okhttp3.HttpUrl.Builder.() -> Unit): String? {
        if (!config.isConfigured) return null
        val base = config.serverUrl.toHttpUrlOrNull() ?: return null
        return base.newBuilder()
            .addPathSegments(path)
            .addQueryParameter("u", config.username)
            .addQueryParameter("t", md5(config.password + salt))
            .addQueryParameter("s", salt)
            .addQueryParameter("v", "1.16.1")
            .addQueryParameter("c", "Kairo")
            .apply(extra)
            .build()
            .toString()
    }

    private fun md5(input: String): String =
        MessageDigest.getInstance("MD5").digest(input.toByteArray())
            .joinToString("") { "%02x".format(it) }
}