package com.miracle.app.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

public final class AuthDtos {

    private AuthDtos() {}

    // request dto

    @Data
    public static class LoginRequest {
        @NotBlank(message = "Username is required")
        private String username;

        @NotBlank(message = "Password is required")
        private String password;
    }

    @Data
    public static class RegisterRequest {
        @NotBlank
        @Size(min = 3, max = 64)
        private String username;

        @NotBlank
        @Size(min = 8, message = "Password must be at least 8 characters")
        private String password;
    }

 //response dto

    @Data
    public static class AuthResponse {
        private final String accessToken;
        private final String tokenType = "Bearer";
        private final String userId;
        private final String username;
        private final List<String> roles;
    }

    @Data
    public static class UserProfileResponse {
        private final String userId;
        private final String username;
        private final List<String> roles;
    }
}
