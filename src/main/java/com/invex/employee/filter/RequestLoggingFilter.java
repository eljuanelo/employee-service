package com.invex.employee.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Component
@Order(SecurityProperties.DEFAULT_FILTER_ORDER - 1)
@Slf4j
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final int MAX_HEADER_LENGTH = 256;
    private static final Pattern UNSAFE_LOG_CHARACTERS = Pattern.compile("[\\p{Cc}\\p{Zl}\\p{Zp}]");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        long startedAt = System.nanoTime();
        try {
            if (log.isDebugEnabled()) {
                log.debug("Request headers Content-Type={} Accept={}",
                        sanitizeHeader(request.getHeader(HttpHeaders.CONTENT_TYPE)),
                        sanitizeHeader(request.getHeader(HttpHeaders.ACCEPT)));
            }

            filterChain.doFilter(request, response);
        } finally {
            long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
            log.info("HTTP {} {} status={} durationMs={}", request.getMethod(), request.getRequestURI(),
                    response.getStatus(), elapsedMillis);
        }
    }

    private String sanitizeHeader(String value) {
        if (value == null) {
            return null;
        }
        String boundedValue = value.substring(0, Math.min(value.length(), MAX_HEADER_LENGTH));
        return UNSAFE_LOG_CHARACTERS.matcher(boundedValue).replaceAll(" ");
    }
}
