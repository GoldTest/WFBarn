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
        val path = config.path.trim().let { 
            if (it.startsWith("/")) it else "/$it" 
        }
        return "$baseUrl$path"
    }

    private fun createClient(config: SyncConfig): HttpClient {
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
                            // 确保 host 匹配，处理可能的 Url 解析异常
                            val configHost = Url(config.url).host
                            request.url.host == configHost
                        } catch (e: Exception) {
                            false
                        }
                    }
                }
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 20000
                connectTimeoutMillis = 20000
            }
        }
    }

    suspend fun upload(config: SyncConfig, state: AppState): Boolean {
        if (config.url.isBlank()) return false
        val fullUrl = getFullUrl(config)
        val client = createClient(config)
        return try {
            // 尝试先创建父目录
            ensureParentDirectoryExists(client, config)
            
            val jsonString = json.encodeToString(AppState.serializer(), state)
            val response: HttpResponse = client.put(fullUrl) {
                setBody(jsonString)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            }
            
            if (!response.status.isSuccess()) {
                val errorMsg = when (response.status.value) {
                    409 -> "HTTP 409 Conflict: 路径冲突，请确保同步路径指向的是文件而非文件夹"
                    401 -> "HTTP 401 Unauthorized: 账号或应用密码错误"
                    else -> "HTTP ${response.status.value}: ${response.status.description}"
                }
                throw Exception(errorMsg)
            }
            true
        } catch (e: Exception) {
            throw e
        } finally {
            client.close()
        }
    }

    private suspend fun ensureParentDirectoryExists(client: HttpClient, config: SyncConfig) {
        val baseUrl = config.url.trimEnd('/')
        val pathParts = config.path.split('/').filter { it.isNotEmpty() }
        
        if (pathParts.size <= 1) return
        
        var currentPath = baseUrl
        for (i in 0 until pathParts.size - 1) {
            currentPath += "/${pathParts[i]}"
            try {
                // WebDAV 规范：某些服务器要求目录以 / 结尾进行 PROPFIND/MKCOL
                val dirPath = "$currentPath/"
                val checkResponse = client.request(dirPath) {
                    method = HttpMethod("PROPFIND")
                    header("Depth", "0")
                }
                
                // 如果目录不存在 (404) 或 路径冲突 (409，通常指父目录未创建)，则尝试创建
                if (checkResponse.status.value == 404 || checkResponse.status.value == 409) {
                    client.request(dirPath) {
                        method = HttpMethod("MKCOL")
                    }
                }
            } catch (e: Exception) {
                // 忽略中间层级的探测错误
            }
        }
    }

    suspend fun download(config: SyncConfig): AppState? {
        if (config.url.isBlank()) return null
        val fullUrl = getFullUrl(config)
        val client = createClient(config)
        return try {
            val response: HttpResponse = client.get(fullUrl)
            when (response.status.value) {
                200 -> {
                    val jsonString = response.bodyAsText()
                    json.decodeFromString<AppState>(jsonString)
                }
                404, 409 -> null // 坚果云在父目录不存在时可能返回 409，统一视为远程无文件
                else -> throw Exception("HTTP ${response.status.value}: ${response.status.description}")
            }
        } catch (e: Exception) {
            throw e
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
            client.head(fullUrl)
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
