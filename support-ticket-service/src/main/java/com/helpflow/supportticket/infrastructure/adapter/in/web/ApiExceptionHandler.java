package com.helpflow.supportticket.infrastructure.adapter.in.web;

import com.helpflow.supportticket.application.port.out.TicketMetricsPort;
import com.helpflow.supportticket.domain.exception.AccountInactiveException;
import com.helpflow.supportticket.domain.exception.BusinessRuleViolationException;
import com.helpflow.supportticket.domain.exception.DomainException;
import com.helpflow.supportticket.domain.exception.DuplicateResourceException;
import com.helpflow.supportticket.domain.exception.ForbiddenOperationException;
import com.helpflow.supportticket.domain.exception.InvalidCredentialsException;
import com.helpflow.supportticket.domain.exception.InvalidTicketTransitionException;
import com.helpflow.supportticket.domain.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Central translation of every failure into an RFC 7807 problem: domain exceptions by type, Bean
 * Validation errors, JPA conflicts, malformed input and unexpected errors.
 */
@RestControllerAdvice
class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    private final ProblemFactory problems;
    private final TicketMetricsPort metrics;

    ApiExceptionHandler(ProblemFactory problems, TicketMetricsPort metrics) {
        this.problems = problems;
        this.metrics = metrics;
    }

    @ExceptionHandler(DomainException.class)
    ResponseEntity<ProblemDetail> handleDomain(DomainException ex, HttpServletRequest request) {
        HttpStatus status = statusOf(ex);
        if (ex instanceof ForbiddenOperationException) {
            metrics.accessDenied();
        }
        log.warn("{} -> {} ({}): {}", ex.getClass().getSimpleName(), status.value(), ex.getCode(), ex.getMessage());
        ProblemDetail problem = problems.create(status, ex.getCode(), LocaleContextHolder.getLocale(),
                request.getRequestURI(), ex.getArgs());
        return ResponseEntity.status(status).body(problem);
    }

    private static HttpStatus statusOf(DomainException ex) {
        if (ex instanceof ResourceNotFoundException) {
            return HttpStatus.NOT_FOUND;
        }
        if (ex instanceof DuplicateResourceException || ex instanceof InvalidTicketTransitionException) {
            return HttpStatus.CONFLICT;
        }
        if (ex instanceof BusinessRuleViolationException) {
            return HttpStatus.UNPROCESSABLE_ENTITY;
        }
        if (ex instanceof ForbiddenOperationException || ex instanceof AccountInactiveException) {
            return HttpStatus.FORBIDDEN;
        }
        if (ex instanceof InvalidCredentialsException) {
            return HttpStatus.UNAUTHORIZED;
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    ResponseEntity<ProblemDetail> handleOptimisticLock(OptimisticLockingFailureException ex,
            HttpServletRequest request) {
        log.warn("Concurrent modification: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problems.create(HttpStatus.CONFLICT,
                "concurrent-modification", LocaleContextHolder.getLocale(), request.getRequestURI()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ProblemDetail> handleDataIntegrity(DataIntegrityViolationException ex,
            HttpServletRequest request) {
        log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problems.create(HttpStatus.CONFLICT,
                "data-integrity", LocaleContextHolder.getLocale(), request.getRequestURI()));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ProblemDetail> handleConstraintViolation(ConstraintViolationException ex,
            HttpServletRequest request) {
        List<Map<String, String>> errors = ex.getConstraintViolations().stream()
                .map(v -> Map.of("field", v.getPropertyPath().toString(), "message", v.getMessage()))
                .toList();
        ProblemDetail problem = problems.create(HttpStatus.BAD_REQUEST, "validation-error",
                LocaleContextHolder.getLocale(), request.getRequestURI());
        problem.setProperty("errors", errors);
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ProblemDetail> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
            HttpServletRequest request) {
        ProblemDetail problem = problems.create(HttpStatus.BAD_REQUEST, "invalid-parameter",
                LocaleContextHolder.getLocale(), request.getRequestURI(), ex.getName());
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(InvalidParameterException.class)
    ResponseEntity<ProblemDetail> handleInvalidParameter(InvalidParameterException ex, HttpServletRequest request) {
        return ResponseEntity.badRequest().body(problems.create(HttpStatus.BAD_REQUEST, "invalid-parameter",
                LocaleContextHolder.getLocale(), request.getRequestURI(), ex.parameter()));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error on {}", request.getRequestURI(), ex);
        return ResponseEntity.internalServerError().body(problems.create(HttpStatus.INTERNAL_SERVER_ERROR,
                "internal-error", LocaleContextHolder.getLocale(), request.getRequestURI()));
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<Map<String, String>> errors = new ArrayList<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> errors.add(
                Map.of("field", error.getField(), "message", String.valueOf(error.getDefaultMessage()))));
        ex.getBindingResult().getGlobalErrors().forEach(error -> errors.add(
                Map.of("field", error.getObjectName(), "message", String.valueOf(error.getDefaultMessage()))));
        ProblemDetail problem = problems.create(HttpStatus.BAD_REQUEST, "validation-error",
                LocaleContextHolder.getLocale(), path(request));
        problem.setProperty("errors", errors);
        return ResponseEntity.badRequest().body(problem);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<Map<String, String>> errors = ex.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> Map.of(
                                "field", result.getMethodParameter().getParameterName() == null ? "parameter"
                                        : result.getMethodParameter().getParameterName(),
                                "message", String.valueOf(error.getDefaultMessage()))))
                .toList();
        ProblemDetail problem = problems.create(HttpStatus.BAD_REQUEST, "validation-error",
                LocaleContextHolder.getLocale(), path(request));
        problem.setProperty("errors", errors);
        return ResponseEntity.badRequest().body(problem);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return ResponseEntity.badRequest().body(problems.create(HttpStatus.BAD_REQUEST, "malformed-request",
                LocaleContextHolder.getLocale(), path(request)));
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
            HttpStatusCode statusCode, WebRequest request) {
        if (body instanceof ProblemDetail detail) {
            problems.decorate(detail, path(request));
        }
        return super.handleExceptionInternal(ex, body, headers, statusCode, request);
    }

    private static String path(WebRequest request) {
        return request instanceof ServletWebRequest web ? web.getRequest().getRequestURI() : null;
    }
}
