package dev.geng.api.trace;

import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;

public final class RequestIds {
    public static final String HEADER = "X-Request-Id";
    private static final String ATTRIBUTE = RequestIds.class.getName();

    private RequestIds() { }

    public static String getOrCreate(HttpServletRequest request) {
        Object value = request.getAttribute(ATTRIBUTE);
        if (value instanceof String existing) {
            return existing;
        }
        String generated = UUID.randomUUID().toString();
        request.setAttribute(ATTRIBUTE, generated);
        return generated;
    }
}
