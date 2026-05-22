package com.controlpro.attendance.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class PunchRequest {
    @NotNull(message = "La latitud es requerida")
    private BigDecimal latitude;

    @NotNull(message = "La longitud es requerida")
    private BigDecimal longitude;
}
