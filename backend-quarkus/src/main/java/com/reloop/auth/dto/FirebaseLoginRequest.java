package com.reloop.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record FirebaseLoginRequest(
    @NotBlank(message = "idToken is required")
    String idToken
) {}
