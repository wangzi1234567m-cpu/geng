package dev.geng.kaogengji.data.diagnostics

import dev.geng.kaogengji.application.diagnostics.BackendHealth
import dev.geng.kaogengji.core.BackendOrigin
import java.net.InetAddress
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer

class HttpBackendHealthGatewayTest {
    private fun withServer(status: Int, content: String, type: String = "application/json", test: (HttpBackendHealthGateway) -> Unit) {
        val server = MockWebServer()
        server.enqueue(MockResponse().setResponseCode(status).setHeader("Content-Type", type).setBody(content))
        server.start(InetAddress.getByName("127.0.0.1"), 0)
        try {
            test(HttpBackendHealthGateway(BackendOrigin.parse("http://127.0.0.1:${server.port}", true)))
            assertEquals("/actuator/health", server.takeRequest(1, TimeUnit.SECONDS)?.path)
        } finally { server.shutdown() }
    }

    @Test fun healthyJsonIsReachable() = withServer(200, "{\"status\":\"UP\"}") {
        assertEquals(BackendHealth.REACHABLE, runBlocking { it.check() })
    }
    @Test fun htmlDoesNotCountAsAHealthyBackend() = withServer(200, "<html>UP</html>", "text/html") {
        assertEquals(BackendHealth.UNREACHABLE, runBlocking { it.check() })
    }
    @Test fun malformedOrOversizedJsonFailsClosed() {
        withServer(200, "{PRIVATE_MARKER") { assertEquals(BackendHealth.UNREACHABLE, runBlocking { it.check() }) }
        withServer(200, " ".repeat(9000)) { assertEquals(BackendHealth.UNREACHABLE, runBlocking { it.check() }) }
    }
    @Test fun downStatusDoesNotCountAsReachable() = withServer(503, "{\"status\":\"DOWN\"}") {
        assertEquals(BackendHealth.UNREACHABLE, runBlocking { it.check() })
    }
    @Test fun redirectsAreNeverFollowed() {
        val server = MockWebServer()
        server.start(InetAddress.getByName("127.0.0.1"), 0)
        server.enqueue(MockResponse().setResponseCode(302).setHeader("Location", "http://127.0.0.1:${server.port}/redirect-target"))
        server.enqueue(MockResponse().setResponseCode(200).setHeader("Content-Type", "application/json").setBody("{\"status\":\"UP\"}"))
        try {
            val client = HttpBackendHealthGateway(BackendOrigin.parse("http://127.0.0.1:${server.port}", true))
            assertEquals(BackendHealth.UNREACHABLE, runBlocking { client.check() })
            assertEquals(1, server.requestCount)
        } finally { server.shutdown() }
    }
}
