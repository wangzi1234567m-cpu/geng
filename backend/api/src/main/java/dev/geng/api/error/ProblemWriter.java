package dev.geng.api.error;

import dev.geng.api.trace.RequestIds;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public final class ProblemWriter {
    private final ObjectMapper mapper;

    public ProblemWriter(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public void write(HttpServletRequest request, HttpServletResponse response,
                      HttpStatus status, String code, String title) throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/problem+json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-store");
        response.setHeader(RequestIds.HEADER, RequestIds.getOrCreate(request));
        mapper.writeValue(response.getOutputStream(), ApiProblems.create(request, status, code, title));
    }
}
