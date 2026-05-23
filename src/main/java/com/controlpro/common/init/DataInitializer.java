package com.controlpro.common.init;

import com.controlpro.auth.model.Role;
import com.controlpro.auth.model.User;
import com.controlpro.auth.repository.UserRepository;
import com.controlpro.common.tenant.TenantContext;
import com.controlpro.employee.model.Employee;
import com.controlpro.employee.repository.EmployeeRepository;
import com.controlpro.schedule.model.EmployeeSchedule;
import com.controlpro.schedule.model.Schedule;
import com.controlpro.schedule.repository.EmployeeScheduleRepository;
import com.controlpro.schedule.repository.ScheduleRepository;
import com.controlpro.tenant.model.Tenant;
import com.controlpro.tenant.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final ScheduleRepository scheduleRepository;
    private final EmployeeScheduleRepository employeeScheduleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        Tenant tenant;
        if (tenantRepository.count() == 0) {
            log.info("Base de datos vacía. Iniciando poblamiento de datos demo...");

            tenant = new Tenant();
            tenant.setName("Empresa Demo ControlPro");
            tenant.setSubdomain("miempresa");
            tenant.setStatus("ACTIVE");
            tenant = tenantRepository.save(tenant);
            log.info("Tenant creado con ID: {}", tenant.getId());
        } else {
            tenant = tenantRepository.findAll().get(0);
            log.info("Tenant existente encontrado: {} con ID: {}", tenant.getName(), tenant.getId());
        }

        UUID tenantId = tenant.getId();
        TenantContext.setCurrentTenant(tenantId);

        try {
            // 3. Crear usuario Admin si no existe
            if (!userRepository.existsByEmail("admin@miempresa.com")) {
                User admin = new User();
                admin.setEmail("admin@miempresa.com");
                admin.setPassword(passwordEncoder.encode("admin123"));
                admin.setRole(Role.ADMIN_EMPRESA);
                admin.setStatus("ACTIVE");
                userRepository.save(admin);
                log.info("Usuario ADMIN creado: admin@miempresa.com / admin123");
            }

            // 4. Crear usuario Empleado si no existe
            User employeeUser;
            Optional<User> empUserOpt = userRepository.findByEmail("empleado@miempresa.com");
            if (empUserOpt.isEmpty()) {
                employeeUser = new User();
                employeeUser.setEmail("empleado@miempresa.com");
                employeeUser.setPassword(passwordEncoder.encode("empleado123"));
                employeeUser.setRole(Role.EMPLEADO);
                employeeUser.setStatus("ACTIVE");
                employeeUser = userRepository.save(employeeUser);
                log.info("Usuario EMPLEADO creado: empleado@miempresa.com / empleado123");
            } else {
                employeeUser = empUserOpt.get();
            }

            // 5. Crear la Ficha de Empleado correspondiente si no existe
            Employee employee;
            Optional<Employee> empOpt = employeeRepository.findByUserId(employeeUser.getId());
            if (empOpt.isEmpty()) {
                employee = new Employee();
                employee.setUser(employeeUser);
                employee.setFirstName("Carlos");
                employee.setLastName("Gómez");
                employee.setDocumentType("DNI");
                employee.setDocumentNumber("74839201");
                employee.setEmail("empleado@miempresa.com");
                employee.setPhone("+51987654321");
                employee.setPosition("Analista Contable");
                employee.setHireDate(LocalDate.now().minusMonths(3));
                employee.setStatus("ACTIVE");
                employee = employeeRepository.save(employee);
                log.info("Ficha de Empleado creada para Carlos Gómez");
            } else {
                employee = empOpt.get();
            }

            // 6. Crear un Horario predeterminado si no existe
            Schedule schedule;
            List<Schedule> schedules = scheduleRepository.findAll();
            if (schedules.isEmpty()) {
                schedule = new Schedule();
                schedule.setName("Horario Administrativo Fijo");
                schedule.setStartTime(LocalTime.of(8, 0));
                schedule.setEndTime(LocalTime.of(17, 0));
                schedule.setToleranceMinutes(10);
                schedule.setDaysOfWeek("1,2,3,4,5"); // Lunes a Viernes
                schedule = scheduleRepository.save(schedule);
                log.info("Plantilla de Horario Fijo creada");
            } else {
                schedule = schedules.get(0);
            }

            // 7. Asignar Horario al Empleado si no tiene uno
            if (employeeScheduleRepository.findActiveScheduleByEmployeeAndDate(employee.getId(), LocalDate.now()).isEmpty()) {
                EmployeeSchedule employeeSchedule = new EmployeeSchedule();
                employeeSchedule.setEmployee(employee);
                employeeSchedule.setSchedule(schedule);
                employeeSchedule.setStartDate(LocalDate.now().minusWeeks(2));
                employeeScheduleRepository.save(employeeSchedule);
                log.info("Horario asignado al empleado");
            }

        } finally {
            // Limpiar contexto del tenant
            TenantContext.clear();
        }

        log.info("Poblamiento de datos inicial / verificación completado.");
    }
}
