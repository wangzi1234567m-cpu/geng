package dev.geng.bootstrap;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.ObjectMapper;
import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("bootstrap")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(ProblemHandlingIntegrationTest.TestEndpoints.class)
class ProblemHandlingIntegrationTest {
    @LocalServerPort int port;
    @Autowired ObjectMapper mapper;

    private void assertProblemContract(HttpResponse<String> response) throws Exception {
        var contract = mapper.readTree(Path.of("../../contracts/openapi.draft.json").toFile());
        var problem = mapper.readTree(response.body());
        for (var field : contract.at("/components/schemas/Problem/required")) {
            assertTrue(problem.has(field.asText()), "Missing contract field: " + field.asText());
        }
        assertEquals(response.statusCode(), problem.path("status").asInt());
        assertEquals(response.headers().firstValue("X-Request-Id").orElseThrow(), problem.path("requestId").asText());
    }

    @TestConfiguration
    static class TestEndpoints {
        @Bean @Order(0)
        SecurityFilterChain testOnlyRoutes(HttpSecurity http) throws Exception {
            return http.securityMatcher("/__test/**").csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll()).build();
        }
        @Bean TestController testController() { return new TestController(); }
    }

    @RestController
    static class TestController {
        record Input(@NotBlank String phrase) { }
        @PostMapping("/__test/validation")
        String validation(@Valid @RequestBody Input body) { return "ok"; }
        @GetMapping("/__test/failure")
        String failure() { throw new IllegalStateException("PRIVATE_INTERNAL_MARKER"); }
    }

    private HttpResponse<String> send(String path, String body) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .header("X-Request-Id", "attacker-controlled");
        if (body == null) { request.GET(); }
        else { request.header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body)); }
        return HttpClient.newHttpClient().send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void errorsUseServerGeneratedCorrelationAndHideInternalExceptionText() throws Exception {
        var response = send("/__test/failure", null);
        assertEquals(500, response.statusCode());
        String requestId = response.headers().firstValue("X-Request-Id").orElseThrow();
        assertDoesNotThrow(() -> UUID.fromString(requestId));
        assertTrue(response.body().contains(requestId));
        assertTrue(response.body().contains("INTERNAL_ERROR"));
        assertFalse(response.body().contains("PRIVATE_INTERNAL_MARKER"));
        assertFalse(response.body().contains("attacker-controlled"));
        assertProblemContract(response);
    }

    @Test
    void invalidInputAndMalformedJsonNeverEchoRequestSecrets() throws Exception {
        var validation = send("/__test/validation", "{\"phrase\":\"\"}");
        assertEquals(422, validation.statusCode());
        assertTrue(validation.body().contains("VALIDATION_FAILED"));
        assertProblemContract(validation);
        var malformed = send("/__test/validation", "{\"phrase\":\"PRIVATE_BODY_MARKER");
        assertEquals(400, malformed.statusCode());
        assertFalse(malformed.body().contains("PRIVATE_BODY_MARKER"));
        assertProblemContract(malformed);
        assertTrue(malformed.headers().firstValue("Content-Type").orElseThrow().contains("application/problem+json"));
    }
}
