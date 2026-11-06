package dev.geng.bootstrap;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ActiveProfiles("bootstrap")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BootstrapSecurityTest {
    @LocalServerPort
    int port;

    private HttpResponse<String> get(String path) throws Exception {
        return HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void onlyMinimalHealthIsPublic() throws Exception {
        var health = get("/actuator/health");
        assertEquals(200, health.statusCode());
        assertFalse(health.body().contains("components"));
        var unauthorized = get("/api/v1/me");
        assertEquals(401, unauthorized.statusCode());
        assertTrue(unauthorized.body().contains("requestId"));
        assertTrue(unauthorized.headers().firstValue("X-Request-Id").isPresent());
        assertEquals(401, get("/actuator/env").statusCode());
    }
}
