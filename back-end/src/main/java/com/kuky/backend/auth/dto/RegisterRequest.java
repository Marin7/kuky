package com.kuky.backend.auth.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @Email(message = "El correo electrónico no es válido.")
        @NotBlank(message = "El correo electrónico es obligatorio.")
        String email,

        @NotBlank(message = "La contraseña es obligatoria.")
        @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres.")
        String password,

        @AssertTrue(message = "Debes aceptar la política de privacidad para crear una cuenta.")
        Boolean gdprConsent
) {}
