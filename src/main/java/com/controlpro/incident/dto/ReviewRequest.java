package com.controlpro.incident.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReviewRequest {
    
    @NotBlank(message = "El estado de la revisión es requerido (APPROVED/REJECTED)")
    private String status;
    
    private String reviewNotes;
}
