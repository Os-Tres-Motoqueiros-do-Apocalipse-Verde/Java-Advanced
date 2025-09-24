package br.com.fiap.otmav.infra.security;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginDto(
        @NotBlank
        @Email
        String email,

        @NotBlank
        String password
) {}
