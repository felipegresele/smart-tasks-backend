package com.task.task.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class AuthDTO {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RegisterRequest {
        @NotBlank(message = "Nome é obrigatório")
        private String name;
        @Email
        @NotBlank(message = "Email é obrigatório")
        private String email;
        @NotBlank(message = "Senha é obrigatória")
        private String password;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class LoginRequest {
        @Email @NotBlank private String email;
        @NotBlank private String password;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class AuthResponse {
        private String token;
        private String name;
        private String email;
        private Long userId;
    }
}
