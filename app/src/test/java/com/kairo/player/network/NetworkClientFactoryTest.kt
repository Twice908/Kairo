package com.kairo.player.network

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkClientFactoryTest {
    private val factory = NetworkClientFactory()

    @Test
    fun configuresExplicitTimeouts() {
        assertEquals(15_000, factory.httpClient.connectTimeoutMillis)
        assertEquals(30_000, factory.httpClient.readTimeoutMillis)
        assertEquals(15_000, factory.httpClient.writeTimeoutMillis)
    }

    @Test
    fun allowsOnlyHttpsBaseUrlsWithTrailingSlash() {
        assertTrue(factory.createRetrofit("https://catalog.example.invalid/api/").baseUrl().isHttps)
        assertThrows(IllegalArgumentException::class.java) {
            factory.createRetrofit("http://catalog.example.invalid/api/")
        }
        assertThrows(IllegalArgumentException::class.java) {
            factory.createRetrofit("https://catalog.example.invalid/api")
        }
    }

    @Test
    fun serializationIgnoresUnknownProviderFields() {
        val decoded = factory.json.decodeFromString<FixturePayload>("""{"name":"local fixture","extra":true}""")

        assertEquals("local fixture", decoded.name)
    }

    @Serializable
    private data class FixturePayload(val name: String)
}