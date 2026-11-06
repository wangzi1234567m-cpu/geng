package dev.geng.api.error;

import dev.geng.domain.error.DomainViolation;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public final class ApiExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(DomainViolation.class)
    ResponseEntity<Object> domain(DomainViolation violation, HttpServletRequest request) {
        HttpStatus status = switch (violation.code()) {
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case ENTRY_EDIT_FORBIDDEN, ENTRY_PERMISSIONS_FORBIDDEN -> HttpStatus.FORBIDDEN;
            default -> HttpStatus.CONFLICT;
        };
        return response(request, status, violation.code().name(), status.getReasonPhrase(), new HttpHeaders());
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception exception, Object body,
            HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        var status = HttpStatus.valueOf(statusCode.value());
        String code = status.is4xxClientError() ? "REQUEST_INVALID" : "INTERNAL_ERROR";
        if (status == HttpStatus.NOT_FOUND) { code = "NOT_FOUND"; }
        if (status == HttpStatus.METHOD_NOT_ALLOWED) { code = "METHOD_NOT_ALLOWED"; }
        if (status == HttpStatus.UNSUPPORTED_MEDIA_TYPE) { code = "MEDIA_TYPE_UNSUPPORTED"; }
        if (status == HttpStatus.BAD_REQUEST && (exception instanceof org.springframework.web.bind.MethodArgumentNotValidException
                || exception instanceof org.springframework.web.method.annotation.HandlerMethodValidationException)) {
            status = HttpStatus.UNPROCESSABLE_ENTITY;
            code = "VALIDATION_FAILED";
        }
        // Never serialize rejected values, raw exception text, paths, or binding objects.
        return response(((ServletWebRequest) request).getRequest(), status, code, status.getReasonPhrase(), headers);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Object> unexpected(Exception exception, HttpServletRequest request) {
        // Do not log exception messages/stacks which can contain payloads or credentials.
        LOG.error("Unhandled API failure class={}", exception.getClass().getSimpleName());
        return response(request, HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Request could not be completed", new HttpHeaders());
    }

    private ResponseEntity<Object> response(HttpServletRequest request, HttpStatus status,
            String code, String title, HttpHeaders originalHeaders) {
        var headers = new HttpHeaders();
        headers.addAll(originalHeaders);
        headers.setContentType(MediaType.APPLICATION_PROBLEM_JSON);
        headers.setCacheControl("no-store");
        return new ResponseEntity<>(ApiProblems.create(request, status, code, title), headers, status);
    }
}
