package com.controlpro.department.controller;

import com.controlpro.department.model.Department;
import com.controlpro.department.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/departments")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DepartmentController {

    private final DepartmentRepository departmentRepository;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN_EMPRESA', 'RRHH')")
    public List<Department> getAllDepartments() {
        return departmentRepository.findAll();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN_EMPRESA', 'RRHH')")
    public ResponseEntity<?> createDepartment(@RequestBody Department department) {
        if (department.getName() == null || department.getName().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "El nombre del departamento es obligatorio"));
        }

        if (departmentRepository.existsByName(department.getName().trim())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Ya existe un departamento con este nombre"));
        }

        department.setName(department.getName().trim());
        Department saved = departmentRepository.save(department);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_EMPRESA', 'RRHH')")
    public ResponseEntity<?> deleteDepartment(@PathVariable Long id) {
        if (!departmentRepository.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Departamento no encontrado"));
        }
        departmentRepository.deleteById(id);
        return ResponseEntity.ok().body(Map.of("message", "Departamento eliminado correctamente"));
    }
}
