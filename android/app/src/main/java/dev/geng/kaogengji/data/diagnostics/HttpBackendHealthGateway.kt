package dev.geng.kaogengji.data.diagnostics

import dev.geng.kaogengji.application.diagnostics.BackendHealth
import dev.geng.kaogengji.application.diagnostics.BackendHealthGateway
import dev.geng.kaogengji.core.BackendOrigin
import java.io.IOException
import java.net.HttpURLConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class HttpBackendHealthGateway(private val origin: BackendOrigin) : BackendHealthGateway {
    override suspend fun check(): BackendHealth = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val http = origin.healthUri.toURL().openConnection() as HttpURLConnection
            connection = http
            http.requestMethod = "GET"
            http.connectTimeout = 3000
            http.readTimeout = 3000
            http.instanceFollowRedirects = false
            http.useCaches = false
            http.setRequestProperty("Accept", "application/json")
            val mediaType = http.contentType?.substringBefore(';')?.lowercase()
            if (http.responseCode != 200 || !(mediaType == "application/json" || mediaType?.endsWith("+json") == true)) {
                return@withContext BackendHealth.UNREACHABLE
            }
            val bytes = http.inputStream.use { input ->
                val buffer = ByteArray(8193)
                var size = 0
                while (size < buffer.size) {
                    val count = input.read(buffer, size, buffer.size - size)
                    if (count < 0) break
                    size += count
                }
                buffer.copyOf(size)
            }
            if (bytes.size > 8192) return@withContext BackendHealth.UNREACHABLE
            val payload = Json.parseToJsonElement(bytes.toString(Charsets.UTF_8)).jsonObject
            if (payload["status"]?.jsonPrimitive?.content == "UP") BackendHealth.REACHABLE else BackendHealth.UNREACHABLE
        } catch (_: IOException) {
            BackendHealth.UNREACHABLE
        } catch (_: IllegalArgumentException) {
            BackendHealth.UNREACHABLE
        } finally {
            connection?.disconnect()
        }
    }
}
