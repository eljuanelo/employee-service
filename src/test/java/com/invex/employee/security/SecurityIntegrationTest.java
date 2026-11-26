package com.invex.employee.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.invex.employee.config.SecurityConfig;
import com.invex.employee.dto.LoginRequest;
import com.invex.employee.dto.LoginResponse;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.security.SecureRandom;
import java.time.Instant;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Value("${app.security.username}")
    private String username;

    @Value("${app.security.password}")
    private String password;

    // Returns HTTP 200 with a Bearer token and its lifetime.
    @Test
    void loginReturnsBearerTokenForValidCredentials() throws Exception {
        login(username, password)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value(not(blankOrNullString())))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600));
    }

    // Returns HTTP 401 with a generic message for a wrong password.
    @Test
    void loginRejectsIncorrectPassword() throws Exception {
        login(username, password + "-incorrect")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username or password."));
    }

    // Returns HTTP 401 with the same message for an unknown user.
    @Test
    void loginRejectsIncorrectUsername() throws Exception {
        login(username + "-unknown", password)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username or password."));
    }

    // Returns HTTP 400 with validation messages for both blank fields.
    @Test
    void loginRejectsBlankCredentials() throws Exception {
        login("", "")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("username: must not be blank")))
                .andExpect(jsonPath("$.message").value(containsString("password: must not be blank")));
    }

    // Returns HTTP 401 when the employee list is requested without a token.
    @Test
    void employeeListRejectsMissingToken() throws Exception {
        mockMvc.perform(get("/employees"))
                .andExpect(status().isUnauthorized());
    }

    // Returns HTTP 200 with a token issued by login.
    @Test
    void employeeListAcceptsTokenIssuedByLogin() throws Exception {
        String token = loginToken();

        mockMvc.perform(get("/employees").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    // Returns HTTP 401 when the issued signature is changed.
    @Test
    void employeeListRejectsTokenWithModifiedSignature() throws Exception {
        String token = loginToken();
        int signatureStart = token.lastIndexOf('.') + 1;
        char replacement = token.charAt(signatureStart) == 'A' ? 'B' : 'A';
        String modifiedToken = token.substring(0, signatureStart) + replacement + token.substring(signatureStart + 1);

        mockMvc.perform(get("/employees").header(HttpHeaders.AUTHORIZATION, "Bearer " + modifiedToken))
                .andExpect(status().isUnauthorized());
    }

    // Returns HTTP 401 for a token past its expiration.
    @Test
    void employeeListRejectsExpiredToken() throws Exception {
        Instant now = Instant.now();
        String token = signToken(jwtEncoder, now.minusSeconds(3600), now.minusSeconds(120));

        mockMvc.perform(get("/employees").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    // Returns HTTP 401 for an otherwise valid token signed with another key.
    @Test
    void employeeListRejectsTokenSignedWithDifferentKey() throws Exception {
        byte[] alternateKey = new byte[32];
        new SecureRandom().nextBytes(alternateKey);
        JwtEncoder alternateEncoder = new NimbusJwtEncoder(new ImmutableSecret<>(alternateKey));
        Instant now = Instant.now();
        String token = signToken(alternateEncoder, now, now.plusSeconds(3600));

        mockMvc.perform(get("/employees").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    // Returns HTTP 401 for employee creation without authentication.
    @Test
    void createEmployeeRejectsUnauthenticatedRequest() throws Exception {
        mockMvc.perform(post("/employees").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    // Returns HTTP 401 for an employee update without authentication.
    @Test
    void updateEmployeeRejectsUnauthenticatedRequest() throws Exception {
        mockMvc.perform(put("/employees/1").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    // Returns HTTP 401 for employee deletion without authentication.
    @Test
    void deleteEmployeeRejectsUnauthenticatedRequest() throws Exception {
        mockMvc.perform(delete("/employees/1"))
                .andExpect(status().isUnauthorized());
    }

    // Returns HTTP 200 with status UP for each public health route.
    @ParameterizedTest
    @ValueSource(strings = {"/actuator/health", "/actuator/health/liveness", "/actuator/health/readiness"})
    void healthEndpointIsPublic(String path) throws Exception {
        mockMvc.perform(get(path))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    // Submits a JSON login request with the supplied credentials.
    private ResultActions login(String username, String password) throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername(username);
        request.setPassword(password);
        return mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request)));
    }

    // Logs in with test credentials and extracts the issued token.
    private String loginToken() throws Exception {
        byte[] body = login(username, password)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        return objectMapper.readValue(body, LoginResponse.class).token();
    }

    // Signs a test JWT with the supplied encoder and timestamps.
    private String signToken(JwtEncoder encoder, Instant issuedAt, Instant expiresAt) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(SecurityConfig.JWT_ISSUER)
                .subject(username)
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
