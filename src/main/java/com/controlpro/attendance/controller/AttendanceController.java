package com.controlpro.attendance.controller;

import com.controlpro.attendance.dto.PunchRequest;
import com.controlpro.attendance.dto.PunchResponse;
import com.controlpro.attendance.model.Attendance;
import com.controlpro.attendance.service.AttendanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/attendance")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AttendanceController {

    private final AttendanceService attendanceService;

    @PostMapping("/punch")
    public ResponseEntity<PunchResponse> punch(@Valid @RequestBody PunchRequest request) {
        PunchResponse response = attendanceService.punch(request.getLatitude(), request.getLongitude());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/today")
    public ResponseEntity<Attendance> getTodayAttendance() {
        Attendance attendance = attendanceService.getTodayAttendance();
        return ResponseEntity.ok(attendance);
    }

    @GetMapping("/my-history")
    public ResponseEntity<List<Attendance>> getMyHistory() {
        List<Attendance> history = attendanceService.getMyHistory();
        return ResponseEntity.ok(history);
    }

    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN_EMPRESA', 'RRHH')")
    public ResponseEntity<List<Attendance>> getAllAttendances() {
        List<Attendance> attendances = attendanceService.getAllAttendances();
        return ResponseEntity.ok(attendances);
    }
}
