package com.wfbarn.service

import com.wfbarn.models.AppState
import com.wfbarn.models.SyncConfig
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

class SyncService {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    private fun getFullUrl(config: SyncConfig): String {
        val baseUrl = config.url.trimEnd('/')
        val path = config.path.let { if (it.startsWith("/")) it else "/$it" }
        return "$baseUrl$path"
    }

    private fun createClient(config: SyncConfig): HttpClient {
        val fullUrl = getFullUrl(config)
        return HttpClient {
            install(ContentNegotiation) {
                json(json)
            }
            install(Auth) {
                basic {
                    credentials {
                        BasicAuthCredentials(config.username, config.password)
                    }
                    sendWithoutRequest { request ->
                        try {
                            request.url.host == Url(fullUrl).host
                        } catch (e: Exception) {
                            false
                        }
                    }
                }
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 15000
                connectTimeoutMillis = 15000
            }
        }
    }

    suspend fun upload(config: SyncConfig, state: AppState): Boolean {
        if (config.url.isBlank()) return false
        val fullUrl = getFullUrl(config)
        val client = createClient(config)
        return try {
            val jsonString = json.encodeToString(AppState.serializer(), state)
            val response: HttpResponse = client.put(fullUrl) {
                setBody(jsonString)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            }
            response.status.isSuccess()
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            client.close()
        }
    }

    suspend fun download(config: SyncConfig): AppState? {
        if (config.url.isBlank()) return null
        val fullUrl = getFullUrl(config)
        val client = createClient(config)
        return try {
            val response: HttpResponse = client.get(fullUrl)
            if (response.status.isSuccess()) {
                val jsonString = response.bodyAsText()
                json.decodeFromString<AppState>(jsonString)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            client.close()
        }
    }

    suspend fun getRemoteLastModified(config: SyncConfig): Long {
        if (config.url.isBlank()) return 0
        val fullUrl = getFullUrl(config)
        val client = createClient(config)
        return try {
            // WebDAV PROPFIND would be better, but HEAD might work if server supports it
            val response: HttpResponse = client.head(fullUrl)
            val lastModified = response.headers[HttpHeaders.LastModified]
            // Simple parsing of HTTP date if needed, or just return 0 for now to force sync
            // For now, let's just return 0 and rely on manual sync/lastSyncTime
            0
        } catch (e: Exception) {
            0
        } finally {
            client.close()
        }
    }
}
