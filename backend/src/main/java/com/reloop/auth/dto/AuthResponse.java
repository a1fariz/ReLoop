package com.reloop.auth.dto;

public record AuthResponse(
    String accessToken,
    String refreshToken,
    Long userId,
    String email,
    String fullName,
    String role
) {}
