package com.controlpro.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {

    @NotBlank(message = "El correo electrónico es requerido")
    @Email(message = "El formato de correo no es válido")
    private String email;

    @NotBlank(message = "La contraseña es requerida")
    private String password;
}
