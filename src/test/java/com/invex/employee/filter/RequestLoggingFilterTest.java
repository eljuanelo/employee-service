package com.invex.employee.filter;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.ServletException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequestLoggingFilterTest {

    private final RequestLoggingFilter filter = new RequestLoggingFilter();
    private final Logger logger = (Logger) LoggerFactory.getLogger(RequestLoggingFilter.class);
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();
    private Level originalLevel;
    private boolean originalAdditivity;

    @BeforeEach
    void captureFilterLogs() {
        originalLevel = logger.getLevel();
        originalAdditivity = logger.isAdditive();
        appender.start();
        logger.addAppender(appender);
        logger.setAdditive(false);
        logger.setLevel(Level.DEBUG);
    }

    @AfterEach
    void restoreLogger() {
        logger.detachAppender(appender);
        logger.setAdditive(originalAdditivity);
        logger.setLevel(originalLevel);
        appender.stop();
    }

    @Test
    void logsAllowedHeadersAndResponseWithoutSensitiveRequestData() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/employees");
        request.setQueryString("password=private-query");
        request.setContent("private-request-body".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        request.addHeader(HttpHeaders.CONTENT_TYPE, "application/json");
        request.addHeader(HttpHeaders.ACCEPT, "application/json");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer private-token");
        request.addHeader(HttpHeaders.COOKIE, "session=private-cookie");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {
            response.setStatus(401);
            response.getWriter().write("private-response-body");
        });

        assertEquals(2, appender.list.size());
        assertEquals(Level.DEBUG, appender.list.get(0).getLevel());
        assertEquals("Request headers Content-Type=application/json Accept=application/json",
                appender.list.get(0).getFormattedMessage());
        assertEquals(Level.INFO, appender.list.get(1).getLevel());
        assertTrue(appender.list.get(1).getFormattedMessage().matches("HTTP POST /employees status=401 durationMs=\\d+"));
    }

    @Test
    void sanitizesAndTruncatesHeaderValues() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/employees");
        request.addHeader(HttpHeaders.ACCEPT, "application/json\r\n\t\u2028" + "x".repeat(300));

        filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> { });

        assertEquals("Request headers Content-Type=null Accept=application/json    " + "x".repeat(236),
                appender.list.get(0).getFormattedMessage());
    }

    @Test
    void logsResponseStatusAndPropagatesChainException() {
        logger.setLevel(Level.INFO);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/employees");
        MockHttpServletResponse response = new MockHttpServletResponse();
        ServletException failure = new ServletException("Chain failure");

        ServletException thrown = assertThrows(ServletException.class, () ->
                filter.doFilter(request, response, (req, res) -> {
                    response.setStatus(503);
                    throw failure;
                }));

        assertSame(failure, thrown);
        assertEquals(1, appender.list.size());
        assertEquals(Level.INFO, appender.list.get(0).getLevel());
        assertTrue(appender.list.get(0).getFormattedMessage().matches("HTTP GET /employees status=503 durationMs=\\d+"));
    }
}
