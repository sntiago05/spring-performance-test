package com.helpflow.dispatch.infrastructure.adapter.in.web;

import com.helpflow.dispatch.application.exception.DispatchApplicationException;
import com.helpflow.dispatch.domain.exception.DomainConflictException;
import com.helpflow.dispatch.domain.exception.DomainRuleViolationException;
import com.helpflow.dispatch.infrastructure.exception.DispatchInfrastructureException;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Translates failures into RFC 9457/7807 {@link ProblemDetail} with localized text,
 * timestamp, trace id, and application/problem+json media type.
 */
@RestControllerAdvice
class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);
    private static final String TYPE_PREFIX = "urn:helpflow:problem:";

    private final MessageSource messages;

    ApiExceptionHandler(MessageSource messages) {
        this.messages = messages;
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> Map.of(
                        "field", error.getField(),
                        "message", String.valueOf(error.getDefaultMessage())))
                .toList();
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "validation-error", request);
        problem.setProperty("errors", errors);
        return response(HttpStatus.BAD_REQUEST, headers, (Object) problem);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return frameworkProblemResponse(HttpStatus.BAD_REQUEST, "malformed-request", headers, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return frameworkProblemResponse(HttpStatus.METHOD_NOT_ALLOWED, "method-not-allowed", headers, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return frameworkProblemResponse(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "unsupported-media-type", headers, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMediaTypeNotAcceptable(HttpMediaTypeNotAcceptableException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return frameworkProblemResponse(HttpStatus.NOT_ACCEPTABLE, "not-acceptable", headers, request);
    }

    @Override
    protected ResponseEntity<Object> handleNoResourceFoundException(NoResourceFoundException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return frameworkProblemResponse(HttpStatus.NOT_FOUND, "resource-not-found", headers, request);
    }

    @Override
    protected ResponseEntity<Object> handleNoHandlerFoundException(NoHandlerFoundException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return frameworkProblemResponse(HttpStatus.NOT_FOUND, "resource-not-found", headers, request);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
            HttpStatusCode statusCode, WebRequest request) {
        if (body instanceof ProblemDetail detail) {
            decorate(detail, request);
            return response(statusCode, headers, (Object) detail);
        }
        HttpStatus resolved = HttpStatus.resolve(statusCode.value());
        HttpStatus status = resolved != null ? resolved : HttpStatus.INTERNAL_SERVER_ERROR;
        return frameworkProblemResponse(status, "internal-error", headers, request);
    }

    @ExceptionHandler(DomainRuleViolationException.class)
    ResponseEntity<ProblemDetail> handleDomainRuleViolation(DomainRuleViolationException ex, WebRequest request) {
        log.warn("Domain rule violation: {}", ex.getMessage());
        return problemResponse(HttpStatus.UNPROCESSABLE_ENTITY, "domain-rule-violation", request);
    }

    @ExceptionHandler(DomainConflictException.class)
    ResponseEntity<ProblemDetail> handleDomainConflict(DomainConflictException ex, WebRequest request) {
        log.warn("Domain state conflict: {}", ex.getMessage());
        return problemResponse(HttpStatus.CONFLICT, "state-conflict", request);
    }

    @ExceptionHandler(DispatchApplicationException.class)
    ResponseEntity<ProblemDetail> handleApplicationException(DispatchApplicationException ex, WebRequest request) {
        log.warn("Application use case failed: {}", ex.getMessage());
        return problemResponse(HttpStatus.UNPROCESSABLE_ENTITY, "application-error", request);
    }

    @ExceptionHandler(DispatchInfrastructureException.class)
    ResponseEntity<ProblemDetail> handleInfrastructureException(DispatchInfrastructureException ex, WebRequest request) {
        log.error("Infrastructure failure", ex);
        return problemResponse(HttpStatus.INTERNAL_SERVER_ERROR, "infrastructure-error", request);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> handleUnexpected(Exception ex, WebRequest request) {
        log.error("Unexpected error", ex);
        return problemResponse(HttpStatus.INTERNAL_SERVER_ERROR, "internal-error", request);
    }

    private ResponseEntity<ProblemDetail> problemResponse(HttpStatus status, String code, WebRequest request) {
        return response(status, new HttpHeaders(), problem(status, code, request));
    }

    private ResponseEntity<Object> frameworkProblemResponse(HttpStatus status, String code, HttpHeaders headers,
            WebRequest request) {
        return response(status, headers, (Object) problem(status, code, request));
    }

    private <T> ResponseEntity<T> response(HttpStatusCode status, HttpHeaders headers, T body) {
        return ResponseEntity.status(status)
                .headers(responseHeaders -> responseHeaders.addAll(headers))
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(body);
    }

    private ProblemDetail problem(HttpStatus status, String code, WebRequest request) {
        var locale = LocaleContextHolder.getLocale();
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status,
                messages.getMessage("problem." + code + ".detail", null, locale));
        problem.setType(URI.create(TYPE_PREFIX + code));
        problem.setTitle(messages.getMessage("problem." + code + ".title", null, locale));
        decorate(problem, request);
        return problem;
    }

    private void decorate(ProblemDetail problem, WebRequest request) {
        if (problem.getInstance() == null) {
            if (request instanceof ServletWebRequest web) {
                problem.setInstance(URI.create(web.getRequest().getRequestURI()));
            } else {
                problem.setInstance(URI.create(""));
            }
        }
        problem.setProperty("timestamp", Instant.now());
        String traceId = MDC.get(TraceIdFilter.MDC_KEY);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }
        problem.setProperty("traceId", traceId);
    }
}
