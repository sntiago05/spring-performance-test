package com.helpflow.supportticket.infrastructure.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.helpflow.supportticket.application.port.out.TicketMetricsPort;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.LocaleResolver;

/**
 * 401 and 403 produced by the security filter chain (they never reach the MVC exception handler), so
 * they are written here as the same localized ProblemDetail the rest of the API returns.
 */
@Component
public class SecurityProblemHandlers implements AuthenticationEntryPoint, AccessDeniedHandler {

    private static final Logger log = LoggerFactory.getLogger(SecurityProblemHandlers.class);

    private final ProblemFactory problems;
    private final LocaleResolver localeResolver;
    private final ObjectMapper objectMapper;
    private final TicketMetricsPort metrics;

    SecurityProblemHandlers(ProblemFactory problems, LocaleResolver localeResolver, ObjectMapper objectMapper,
            TicketMetricsPort metrics) {
        this.problems = problems;
        this.localeResolver = localeResolver;
        this.objectMapper = objectMapper;
        this.metrics = metrics;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        metrics.authenticationFailed("unauthenticated");
        log.warn("Unauthenticated request to {} {}: {}", request.getMethod(), request.getRequestURI(),
                authException.getMessage());
        write(request, response, HttpStatus.UNAUTHORIZED, "unauthorized");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException {
        metrics.accessDenied();
        log.warn("Access denied to {} {}", request.getMethod(), request.getRequestURI());
        write(request, response, HttpStatus.FORBIDDEN, "forbidden");
    }

    private void write(HttpServletRequest request, HttpServletResponse response, HttpStatus status, String code)
            throws IOException {
        ProblemDetail problem = problems.create(status, code, localeResolver.resolveLocale(request),
                request.getRequestURI());
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        if (status == HttpStatus.UNAUTHORIZED) {
            response.setHeader("WWW-Authenticate", "Bearer");
        }
        objectMapper.writeValue(response.getOutputStream(), problem);
    }
}
