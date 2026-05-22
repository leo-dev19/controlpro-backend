package com.controlpro.attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PunchResponse {
    private Long attendanceId;
    private String punchType; // CHECK_IN, CHECK_OUT
    private LocalDateTime timestamp;
    private String status; // PUNCTUAL, LATE, ABSENT, JUSTIFIED
    private Integer minutesLate;
    private String message;
}
