package com.controlpro.attendance.model;

import com.controlpro.common.model.AuditableEntity;
import com.controlpro.employee.model.Employee;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "attendances")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Getter
@Setter
public class Attendance extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TenantId
    @Column(name = "tenant_id", nullable = false, columnDefinition = "UUID")
    private UUID tenantId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "check_in")
    private LocalDateTime checkIn;

    @Column(name = "check_out")
    private LocalDateTime checkOut;

    @Column(nullable = false, length = 20)
    private String status; // PUNCTUAL, LATE, ABSENT, JUSTIFIED

    @Column(name = "minutes_late")
    private Integer minutesLate = 0;

    @Column(name = "latitude_in")
    private BigDecimal latitudeIn;

    @Column(name = "longitude_in")
    private BigDecimal longitudeIn;

    @Column(name = "latitude_out")
    private BigDecimal latitudeOut;

    @Column(name = "longitude_out")
    private BigDecimal longitudeOut;
}
