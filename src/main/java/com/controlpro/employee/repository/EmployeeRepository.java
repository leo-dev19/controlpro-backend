package com.controlpro.employee.repository;

import com.controlpro.employee.model.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    Optional<Employee> findByEmail(String email);
    Optional<Employee> findByUserId(UUID userId);
    boolean existsByDocumentTypeAndDocumentNumber(String documentType, String documentNumber);
}
