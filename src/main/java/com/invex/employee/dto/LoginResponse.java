package com.invex.employee.dto;

public record LoginResponse(String token, String tokenType, long expiresIn) {
}
