package com.controlpro.employee.controller;

import com.controlpro.auth.model.Role;
import com.controlpro.auth.model.User;
import com.controlpro.auth.repository.UserRepository;
import com.controlpro.employee.model.Employee;
import com.controlpro.employee.repository.EmployeeRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/employees")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class EmployeeController {

    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN_EMPRESA', 'RRHH')")
    public List<Employee> getAllEmployees() {
        return employeeRepository.findAll();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN_EMPRESA', 'RRHH')")
    @Transactional
    public ResponseEntity<?> createEmployee(@RequestBody EmployeeCreateRequest request) {
        if (employeeRepository.existsByDocumentTypeAndDocumentNumber(request.getDocumentType(), request.getDocumentNumber())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Ya existe un empleado con ese tipo y número de documento"));
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.badRequest().body(Map.of("message", "El correo electrónico ya está registrado"));
        }

        // 1. Crear el usuario asociado
        User user = new User();
        user.setEmail(request.getEmail().trim());
        // Contraseña por defecto basada en el número de documento si no se provee
        String rawPassword = request.getPassword() != null && !request.getPassword().trim().isEmpty()
                ? request.getPassword().trim()
                : request.getDocumentNumber().trim();
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(Role.EMPLEADO);
        user.setStatus("ACTIVE");
        user = userRepository.save(user);

        // 2. Crear la ficha de empleado
        Employee employee = new Employee();
        employee.setUser(user);
        employee.setFirstName(request.getFirstName().trim());
        employee.setLastName(request.getLastName().trim());
        employee.setDocumentType(request.getDocumentType());
        employee.setDocumentNumber(request.getDocumentNumber().trim());
        employee.setEmail(request.getEmail().trim());
        employee.setPhone(request.getPhone() != null ? request.getPhone().trim() : null);
        employee.setDepartmentId(request.getDepartmentId());
        employee.setPosition(request.getPosition() != null ? request.getPosition().trim() : null);
        employee.setHireDate(LocalDate.parse(request.getHireDate(), DateTimeFormatter.ISO_LOCAL_DATE));
        employee.setStatus("ACTIVE");

        Employee saved = employeeRepository.save(employee);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_EMPRESA', 'RRHH')")
    @Transactional
    public ResponseEntity<?> deleteEmployee(@PathVariable Long id) {
        return employeeRepository.findById(id).map(employee -> {
            // Eliminar empleado
            employeeRepository.delete(employee);
            // Eliminar usuario asociado si existe
            if (employee.getUser() != null) {
                userRepository.delete(employee.getUser());
            }
            return ResponseEntity.ok().body(Map.of("message", "Empleado y su cuenta asociada eliminados correctamente"));
        }).orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Empleado no encontrado")));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN_EMPRESA', 'RRHH')")
    @Transactional
    public ResponseEntity<?> updateEmployeeStatus(@PathVariable Long id, @RequestBody Map<String, String> statusUpdate) {
        String newStatus = statusUpdate.get("status");
        if (newStatus == null || (!newStatus.equals("ACTIVE") && !newStatus.equals("INACTIVE"))) {
            return ResponseEntity.badRequest().body(Map.of("message", "Estado inválido. Debe ser ACTIVE o INACTIVE."));
        }

        return employeeRepository.findById(id).map(employee -> {
            employee.setStatus(newStatus);
            employeeRepository.save(employee);

            // Actualizar también el estado del usuario asociado si existe
            if (employee.getUser() != null) {
                User user = employee.getUser();
                user.setStatus(newStatus);
                userRepository.save(user);
            }

            return ResponseEntity.ok().body(Map.of(
                    "message", "Estado del empleado actualizado correctamente",
                    "status", newStatus
            ));
        }).orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Empleado no encontrado")));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_EMPRESA', 'RRHH')")
    @Transactional
    public ResponseEntity<?> updateEmployee(@PathVariable Long id, @RequestBody EmployeeCreateRequest request) {
        return employeeRepository.findById(id).map(employee -> {
            // Validar si el nuevo documento ya existe en otro empleado
            if (!employee.getDocumentNumber().equals(request.getDocumentNumber()) || !employee.getDocumentType().equals(request.getDocumentType())) {
                if (employeeRepository.existsByDocumentTypeAndDocumentNumber(request.getDocumentType(), request.getDocumentNumber())) {
                    return ResponseEntity.badRequest().body(Map.of("message", "Ya existe otro empleado con ese tipo y número de documento"));
                }
            }

            // Validar si el correo ya existe en otro usuario
            if (!employee.getEmail().equalsIgnoreCase(request.getEmail())) {
                if (userRepository.existsByEmail(request.getEmail())) {
                    return ResponseEntity.badRequest().body(Map.of("message", "El correo electrónico ya está registrado por otro usuario"));
                }
            }

            // Actualizar datos del usuario asociado si existe
            if (employee.getUser() != null) {
                User user = employee.getUser();
                user.setEmail(request.getEmail().trim());
                if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
                    user.setPassword(passwordEncoder.encode(request.getPassword().trim()));
                }
                userRepository.save(user);
            }

            // Actualizar ficha de empleado
            employee.setFirstName(request.getFirstName().trim());
            employee.setLastName(request.getLastName().trim());
            employee.setDocumentType(request.getDocumentType());
            employee.setDocumentNumber(request.getDocumentNumber().trim());
            employee.setEmail(request.getEmail().trim());
            employee.setPhone(request.getPhone() != null ? request.getPhone().trim() : null);
            employee.setDepartmentId(request.getDepartmentId());
            employee.setPosition(request.getPosition() != null ? request.getPosition().trim() : null);
            employee.setHireDate(LocalDate.parse(request.getHireDate(), DateTimeFormatter.ISO_LOCAL_DATE));

            Employee saved = employeeRepository.save(employee);
            return ResponseEntity.ok(saved);
        }).orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Empleado no encontrado")));
    }



    @Data
    public static class EmployeeCreateRequest {
        private String firstName;
        private String lastName;
        private String email;
        private String documentType;
        private String documentNumber;
        private String phone;
        private Long departmentId;
        private String position;
        private String hireDate;
        private String password;
    }
}
