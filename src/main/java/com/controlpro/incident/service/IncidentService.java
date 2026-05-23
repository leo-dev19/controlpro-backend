package com.controlpro.incident.service;

import com.controlpro.attendance.model.Attendance;
import com.controlpro.attendance.repository.AttendanceRepository;
import com.controlpro.auth.model.User;
import com.controlpro.auth.repository.UserRepository;
import com.controlpro.employee.model.Employee;
import com.controlpro.employee.repository.EmployeeRepository;
import com.controlpro.incident.dto.JustifyRequest;
import com.controlpro.incident.dto.ReviewRequest;
import com.controlpro.incident.model.Incident;
import com.controlpro.incident.repository.IncidentRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final AttendanceRepository attendanceRepository;

    private Employee getCurrentEmployee() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado: " + email));
        return employeeRepository.findByUserId(user.getId())
                .orElseThrow(() -> new EntityNotFoundException("Ficha de empleado no encontrada para el usuario: " + email));
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado: " + email));
    }

    public List<Incident> getMyIncidents() {
        Employee employee = getCurrentEmployee();
        return incidentRepository.findByEmployeeId(employee.getId());
    }

    @Transactional
    public Incident justifyIncident(Long id, JustifyRequest request) {
        Employee employee = getCurrentEmployee();
        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Incidencia no encontrada: " + id));

        if (!incident.getEmployee().getId().equals(employee.getId())) {
            throw new IllegalArgumentException("No estás autorizado para justificar esta incidencia.");
        }

        incident.setReason(request.getReason());
        incident.setAttachmentUrl(request.getAttachmentUrl());
        incident.setStatus("PENDING");

        return incidentRepository.save(incident);
    }

    public List<Incident> getAllIncidents() {
        return incidentRepository.findAll();
    }

    @Transactional
    public Incident reviewIncident(Long id, ReviewRequest request) {
        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Incidencia no encontrada: " + id));

        String targetStatus = request.getStatus().toUpperCase();
        if (!"APPROVED".equals(targetStatus) && !"REJECTED".equals(targetStatus)) {
            throw new IllegalArgumentException("Estado inválido. Debe ser APPROVED o REJECTED.");
        }

        User reviewer = getCurrentUser();
        incident.setStatus(targetStatus);
        incident.setReviewedBy(reviewer.getId());
        incident.setReviewNotes(request.getReviewNotes());

        if ("APPROVED".equals(targetStatus) && incident.getAttendance() != null) {
            Attendance attendance = incident.getAttendance();
            attendance.setStatus("JUSTIFIED");
            attendanceRepository.save(attendance);
        }

        return incidentRepository.save(incident);
    }
}
