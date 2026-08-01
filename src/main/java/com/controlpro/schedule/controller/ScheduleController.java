package com.controlpro.schedule.controller;

import com.controlpro.employee.model.Employee;
import com.controlpro.employee.repository.EmployeeRepository;
import com.controlpro.schedule.model.EmployeeSchedule;
import com.controlpro.schedule.model.Schedule;
import com.controlpro.schedule.repository.EmployeeScheduleRepository;
import com.controlpro.schedule.repository.ScheduleRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/schedules")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ScheduleController {

    private final ScheduleRepository scheduleRepository;
    private final EmployeeScheduleRepository employeeScheduleRepository;
    private final EmployeeRepository employeeRepository;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN_EMPRESA', 'RRHH')")
    public List<Schedule> getAllSchedules() {
        return scheduleRepository.findAll();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN_EMPRESA', 'RRHH')")
    public ResponseEntity<?> createSchedule(@RequestBody Schedule schedule) {
        if (schedule.getName() == null || schedule.getName().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "El nombre de la plantilla de horario es obligatorio"));
        }
        if (schedule.getStartTime() == null || schedule.getEndTime() == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Las horas de inicio y fin son obligatorias"));
        }
        if (schedule.getDaysOfWeek() == null || schedule.getDaysOfWeek().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Debe especificar los días de la semana laborables"));
        }

        schedule.setName(schedule.getName().trim());
        Schedule saved = scheduleRepository.save(schedule);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_EMPRESA', 'RRHH')")
    public ResponseEntity<?> deleteSchedule(@PathVariable Long id) {
        if (!scheduleRepository.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Horario no encontrado"));
        }
        scheduleRepository.deleteById(id);
        return ResponseEntity.ok().body(Map.of("message", "Horario eliminado correctamente"));
    }

    @PostMapping("/assign")
    @PreAuthorize("hasAnyRole('ADMIN_EMPRESA', 'RRHH')")
    public ResponseEntity<?> assignScheduleToEmployee(@RequestBody AssignmentRequest request) {
        Optional<Employee> empOpt = employeeRepository.findById(request.getEmployeeId());
        if (empOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Empleado no encontrado"));
        }

        Optional<Schedule> schOpt = scheduleRepository.findById(request.getScheduleId());
        if (schOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Horario no encontrado"));
        }

        Employee employee = empOpt.get();
        Schedule schedule = schOpt.get();

        LocalDate start = LocalDate.parse(request.getStartDate(), DateTimeFormatter.ISO_LOCAL_DATE);
        LocalDate end = null;
        if (request.getEndDate() != null && !request.getEndDate().trim().isEmpty()) {
            end = LocalDate.parse(request.getEndDate(), DateTimeFormatter.ISO_LOCAL_DATE);
        }

        // Crear asignación
        EmployeeSchedule employeeSchedule = new EmployeeSchedule();
        employeeSchedule.setEmployee(employee);
        employeeSchedule.setSchedule(schedule);
        employeeSchedule.setStartDate(start);
        employeeSchedule.setEndDate(end);

        EmployeeSchedule saved = employeeScheduleRepository.save(employeeSchedule);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping("/assignments")
    @PreAuthorize("hasAnyRole('ADMIN_EMPRESA', 'RRHH')")
    public List<EmployeeSchedule> getAllAssignments() {
        return employeeScheduleRepository.findAll();
    }

    @GetMapping("/my-schedule")
    public ResponseEntity<?> getMySchedule() {
        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        Employee employee = employeeRepository.findByEmail(email)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Ficha de empleado no encontrada para el usuario: " + email));

        Optional<EmployeeSchedule> activeScheduleOpt = employeeScheduleRepository
                .findActiveScheduleByEmployeeAndDate(employee.getId(), LocalDate.now());

        if (activeScheduleOpt.isPresent()) {
            return ResponseEntity.ok(activeScheduleOpt.get().getSchedule());
        } else {
            return ResponseEntity.noContent().build();
        }
    }

    @Data
    public static class AssignmentRequest {
        private Long employeeId;
        private Long scheduleId;
        private String startDate;
        private String endDate;
    }
}
