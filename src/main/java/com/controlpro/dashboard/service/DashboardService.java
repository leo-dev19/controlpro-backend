package com.controlpro.dashboard.service;

import com.controlpro.attendance.model.Attendance;
import com.controlpro.attendance.repository.AttendanceRepository;
import com.controlpro.dashboard.dto.DashboardMetricsResponse;
import com.controlpro.department.model.Department;
import com.controlpro.department.repository.DepartmentRepository;
import com.controlpro.employee.model.Employee;
import com.controlpro.employee.repository.EmployeeRepository;
import com.controlpro.incident.repository.IncidentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final AttendanceRepository attendanceRepository;
    private final IncidentRepository incidentRepository;

    public DashboardMetricsResponse getMetrics() {
        LocalDate today = LocalDate.now();

        // 1. Obtener empleados activos y departamentos
        List<Employee> activeEmployees = employeeRepository.findAll().stream()
                .filter(e -> "ACTIVE".equals(e.getStatus()))
                .collect(Collectors.toList());

        List<Department> departments = departmentRepository.findAll();
        Map<Long, String> departmentNameMap = departments.stream()
                .collect(Collectors.toMap(Department::getId, Department::getName));

        // 2. Obtener asistencias de hoy
        List<Attendance> todayAttendances = attendanceRepository.findByDate(today);
        Map<Long, Attendance> attendanceByEmployeeId = todayAttendances.stream()
                .collect(Collectors.toMap(a -> a.getEmployee().getId(), a -> a));

        // 3. Calcular KPIs principales
        long totalEmployees = activeEmployees.size();
        long presentToday = todayAttendances.stream()
                .filter(a -> a.getCheckIn() != null)
                .count();

        long lateToday = todayAttendances.stream()
                .filter(a -> "LATE".equals(a.getStatus()))
                .count();

        long absentToday = Math.max(0, totalEmployees - presentToday);

        long pendingJustifications = incidentRepository.countByStatus("PENDING");

        double attendanceRate = totalEmployees > 0 
                ? Math.round(((double) presentToday / totalEmployees) * 1000.0) / 10.0 
                : 0.0;

        // 4. Agrupar estadísticas por departamento
        List<DashboardMetricsResponse.DepartmentStat> deptStats = new ArrayList<>();
        Map<Long, List<Employee>> employeesByDept = activeEmployees.stream()
                .filter(e -> e.getDepartmentId() != null)
                .collect(Collectors.groupingBy(Employee::getDepartmentId));

        for (Department dept : departments) {
            List<Employee> deptEmployees = employeesByDept.getOrDefault(dept.getId(), new ArrayList<>());
            long deptTotal = deptEmployees.size();

            long punctual = 0;
            long late = 0;
            long absent = 0;

            for (Employee emp : deptEmployees) {
                Attendance att = attendanceByEmployeeId.get(emp.getId());
                if (att != null) {
                    if ("LATE".equals(att.getStatus())) {
                        late++;
                    } else {
                        punctual++; // PUNCTUAL o JUSTIFIED se consideran cubiertos
                    }
                } else {
                    absent++;
                }
            }

            // Evitar agregar si no tiene personal para no saturar el gráfico del MVP
            if (deptTotal > 0) {
                deptStats.add(DashboardMetricsResponse.DepartmentStat.builder()
                        .area(dept.getName())
                        .punctual(punctual)
                        .late(late)
                        .absent(absent)
                        .build());
            }
        }

        // 5. Construir feed de actividad en tiempo real (últimos 5 fichajes de hoy)
        List<DashboardMetricsResponse.LiveFeedEntry> liveFeed = new ArrayList<>();
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

        // Ordenar asistencias por fecha/hora de fichaje de forma descendente
        List<Attendance> sortedAttendances = todayAttendances.stream()
                .sorted((a1, a2) -> {
                    // Ordenar por el último evento temporal registrado (salida o entrada)
                    java.time.LocalDateTime t1 = a1.getCheckOut() != null ? a1.getCheckOut() : a1.getCheckIn();
                    java.time.LocalDateTime t2 = a2.getCheckOut() != null ? a2.getCheckOut() : a2.getCheckIn();
                    return t2.compareTo(t1);
                })
                .limit(5)
                .collect(Collectors.toList());

        for (Attendance att : sortedAttendances) {
            Employee emp = att.getEmployee();
            String name = emp.getFirstName() + " " + emp.getLastName();
            String area = emp.getDepartmentId() != null ? departmentNameMap.getOrDefault(emp.getDepartmentId(), "Sin Área") : "Sin Área";
            
            boolean hasCheckedOut = att.getCheckOut() != null;
            String type = hasCheckedOut ? "Salida" : "Entrada";
            String timeStr = hasCheckedOut 
                    ? att.getCheckOut().format(timeFormatter) 
                    : att.getCheckIn().format(timeFormatter);

            liveFeed.add(DashboardMetricsResponse.LiveFeedEntry.builder()
                    .name(name)
                    .area(area)
                    .type(type)
                    .time(timeStr)
                    .status(att.getStatus())
                    .minutesLate(att.getMinutesLate())
                    .build());
        }

        return DashboardMetricsResponse.builder()
                .totalEmployees(totalEmployees)
                .presentToday(presentToday)
                .lateToday(lateToday)
                .absentToday(absentToday)
                .pendingJustifications(pendingJustifications)
                .attendanceRate(attendanceRate)
                .departmentStats(deptStats)
                .liveFeed(liveFeed)
                .build();
    }
}
