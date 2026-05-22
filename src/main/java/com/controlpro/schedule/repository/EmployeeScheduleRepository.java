package com.controlpro.schedule.repository;

import com.controlpro.schedule.model.EmployeeSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeScheduleRepository extends JpaRepository<EmployeeSchedule, Long> {

    @Query("SELECT es FROM EmployeeSchedule es WHERE es.employee.id = :employeeId " +
           "AND es.startDate <= :date AND (es.endDate IS NULL OR es.endDate >= :date)")
    Optional<EmployeeSchedule> findActiveScheduleByEmployeeAndDate(
            @Param("employeeId") Long employeeId,
            @Param("date") LocalDate date
    );

    List<EmployeeSchedule> findByEmployeeId(Long employeeId);
}
