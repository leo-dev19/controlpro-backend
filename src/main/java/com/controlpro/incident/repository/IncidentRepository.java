package com.controlpro.incident.repository;

import com.controlpro.incident.model.Incident;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, Long> {
    List<Incident> findByEmployeeId(Long employeeId);
    Optional<Incident> findByAttendanceId(Long attendanceId);
    long countByStatus(String status);
}
