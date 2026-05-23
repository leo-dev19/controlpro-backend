package com.controlpro.incident.controller;

import com.controlpro.incident.dto.JustifyRequest;
import com.controlpro.incident.dto.ReviewRequest;
import com.controlpro.incident.model.Incident;
import com.controlpro.incident.service.IncidentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/incidents")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class IncidentController {

    private final IncidentService incidentService;

    @GetMapping("/my-incidents")
    public ResponseEntity<List<Incident>> getMyIncidents() {
        return ResponseEntity.ok(incidentService.getMyIncidents());
    }

    @PostMapping("/{id}/justify")
    public ResponseEntity<Incident> justifyIncident(
            @PathVariable Long id,
            @Valid @RequestBody JustifyRequest request) {
        return ResponseEntity.ok(incidentService.justifyIncident(id, request));
    }

    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN_EMPRESA', 'RRHH')")
    public ResponseEntity<List<Incident>> getAllIncidents() {
        return ResponseEntity.ok(incidentService.getAllIncidents());
    }

    @PostMapping("/{id}/review")
    @PreAuthorize("hasAnyRole('ADMIN_EMPRESA', 'RRHH')")
    public ResponseEntity<Incident> reviewIncident(
            @PathVariable Long id,
            @Valid @RequestBody ReviewRequest request) {
        return ResponseEntity.ok(incidentService.reviewIncident(id, request));
    }
}
