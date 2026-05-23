package com.controlpro.schedule.model;

import com.controlpro.common.model.AuditableEntity;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.TenantId;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "schedules")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Getter
@Setter
public class Schedule extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TenantId
    @Column(name = "tenant_id", nullable = false, columnDefinition = "UUID")
    private UUID tenantId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "tolerance_minutes", nullable = false)
    private Integer toleranceMinutes = 0;

    @Column(name = "days_of_week", nullable = false, length = 50)
    private String daysOfWeek; // Separado por comas, ej: "1,2,3,4,5"
}
