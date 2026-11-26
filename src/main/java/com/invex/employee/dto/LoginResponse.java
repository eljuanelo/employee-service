package com.invex.employee.dto;

import io.swagger.v3.oas.annotations.media.Schema;
public record LoginResponse(
        @Schema(description = "Signed JWT for the Authorization header.") String token,
        @Schema(example = "Bearer") String tokenType,
        @Schema(description = "Token lifetime in seconds.", example = "3600") long expiresIn) {
}
