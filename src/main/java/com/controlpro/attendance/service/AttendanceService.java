package com.controlpro.attendance.service;

import com.controlpro.attendance.dto.PunchResponse;
import com.controlpro.attendance.model.Attendance;
import com.controlpro.attendance.repository.AttendanceRepository;
import com.controlpro.auth.model.User;
import com.controlpro.auth.repository.UserRepository;
import com.controlpro.employee.model.Employee;
import com.controlpro.employee.repository.EmployeeRepository;
import com.controlpro.incident.model.Incident;
import com.controlpro.incident.repository.IncidentRepository;
import com.controlpro.schedule.model.EmployeeSchedule;
import com.controlpro.schedule.model.Schedule;
import com.controlpro.schedule.repository.EmployeeScheduleRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final EmployeeScheduleRepository employeeScheduleRepository;
    private final IncidentRepository incidentRepository;

    @Transactional
    public PunchResponse punch(BigDecimal latitude, BigDecimal longitude) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado: " + email));
        
        Employee employee = employeeRepository.findByUserId(user.getId())
                .orElseThrow(() -> new EntityNotFoundException("Ficha de empleado no encontrada para el usuario: " + email));

        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        Optional<Attendance> attendanceOpt = attendanceRepository.findByEmployeeIdAndDate(employee.getId(), today);

        if (attendanceOpt.isPresent()) {
            Attendance attendance = attendanceOpt.get();
            if (attendance.getCheckOut() != null) {
                throw new IllegalArgumentException("Ya has registrado tus marcaciones de entrada y salida para el día de hoy.");
            }
            // Registrar salida
            attendance.setCheckOut(now);
            attendance.setLatitudeOut(latitude);
            attendance.setLongitudeOut(longitude);
            attendanceRepository.save(attendance);

            return PunchResponse.builder()
                    .attendanceId(attendance.getId())
                    .punchType("CHECK_OUT")
                    .timestamp(now)
                    .status(attendance.getStatus())
                    .minutesLate(attendance.getMinutesLate())
                    .message("Marcación de salida registrada exitosamente.")
                    .build();
        } else {
            // Registrar entrada
            Attendance attendance = new Attendance();
            attendance.setEmployee(employee);
            attendance.setDate(today);
            attendance.setCheckIn(now);
            attendance.setLatitudeIn(latitude);
            attendance.setLongitudeIn(longitude);

            // Calcular tardanza
            Optional<EmployeeSchedule> activeScheduleOpt = employeeScheduleRepository
                    .findActiveScheduleByEmployeeAndDate(employee.getId(), today);

            if (activeScheduleOpt.isEmpty()) {
                throw new IllegalArgumentException("No tienes un horario activo asignado para el día de hoy. Por favor contacta a tu administrador.");
            }

            String status = "PUNCTUAL";
            int minutesLate = 0;
            Schedule schedule = activeScheduleOpt.get().getSchedule();
            int dayOfWeek = today.getDayOfWeek().getValue(); // 1 = Lunes, 7 = Domingo
            String dayStr = String.valueOf(dayOfWeek);
            boolean isScheduledToday = Arrays.asList(schedule.getDaysOfWeek().split(","))
                    .contains(dayStr);

            if (isScheduledToday) {
                LocalTime scheduledStartTime = schedule.getStartTime();
                LocalDateTime scheduledCheckInDateTime = LocalDateTime.of(today, scheduledStartTime);

                if (now.isAfter(scheduledCheckInDateTime)) {
                    long diffMinutes = Duration.between(scheduledCheckInDateTime, now).toMinutes();
                    if (diffMinutes > schedule.getToleranceMinutes()) {
                        status = "LATE";
                        minutesLate = (int) diffMinutes;
                    }
                }
            }

            attendance.setStatus(status);
            attendance.setMinutesLate(minutesLate);
            attendance = attendanceRepository.save(attendance);

            // Si es tardanza, generar incidencia automática
            if ("LATE".equals(status)) {
                Incident incident = new Incident();
                incident.setEmployee(employee);
                incident.setAttendance(attendance);
                incident.setType("TARDANZA");
                incident.setDate(today);
                incident.setReason("Tardanza automática de " + minutesLate + " minutos en el ingreso.");
                incident.setStatus("PENDING");
                incidentRepository.save(incident);
            }

            String msg = "LATE".equals(status) 
                    ? "Marcación de entrada registrada como TARDANZA." 
                    : "Marcación de entrada registrada exitosamente.";

            return PunchResponse.builder()
                    .attendanceId(attendance.getId())
                    .punchType("CHECK_IN")
                    .timestamp(now)
                    .status(status)
                    .minutesLate(minutesLate)
                    .message(msg)
                    .build();
        }
    }

    public Attendance getTodayAttendance() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado: " + email));
        
        Employee employee = employeeRepository.findByUserId(user.getId())
                .orElseThrow(() -> new EntityNotFoundException("Ficha de empleado no encontrada para el usuario: " + email));

        return attendanceRepository.findByEmployeeIdAndDate(employee.getId(), LocalDate.now()).orElse(null);
    }

    public List<Attendance> getMyHistory() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado: " + email));
        
        Employee employee = employeeRepository.findByUserId(user.getId())
                .orElseThrow(() -> new EntityNotFoundException("Ficha de empleado no encontrada para el usuario: " + email));

        return attendanceRepository.findByEmployeeIdOrderByDateDesc(employee.getId());
    }

    public List<Attendance> getAllAttendances() {
        return attendanceRepository.findAll();
    }
}
