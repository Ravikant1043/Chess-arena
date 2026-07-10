package com.chessarena.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request/response payloads for the auth endpoints, grouped as nested records to keep
 * the small DTOs together. Bean-Validation annotations enforce basic input rules before
 * the controller runs.
 */
public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank @Size(min = 3, max = 30) String username,
            @NotBlank @Size(min = 6, max = 100) String password) {
    }

    public record LoginRequest(
            @NotBlank String username,
            @NotBlank String password) {
    }

    /** Returned on successful register/login: the bearer token plus a bit of profile data. */
    public record AuthResponse(
            String token,
            String username,
            int score) {
    }
}
