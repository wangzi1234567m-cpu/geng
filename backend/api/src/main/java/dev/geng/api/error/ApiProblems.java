package dev.geng.api.error;

import dev.geng.api.trace.RequestIds;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

public final class ApiProblems {
    private ApiProblems() { }

    public static ProblemDetail create(HttpServletRequest request, HttpStatus status, String code, String title) {
        var problem = ProblemDetail.forStatusAndDetail(status, title);
        problem.setTitle(title);
        problem.setType(URI.create("urn:gengbook:problem:" + code.toLowerCase(Locale.ROOT).replace('_', '-')));
        problem.setProperty("code", code);
        problem.setProperty("requestId", RequestIds.getOrCreate(request));
        return problem;
    }
}
