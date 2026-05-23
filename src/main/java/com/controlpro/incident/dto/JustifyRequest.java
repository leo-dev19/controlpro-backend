package com.controlpro.incident.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JustifyRequest {
    
    @NotBlank(message = "El motivo de justificación es requerido")
    private String reason;
    
    private String attachmentUrl;
}
