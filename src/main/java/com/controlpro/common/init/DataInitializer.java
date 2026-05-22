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
        if (tenantRepository.count() == 0) {
            log.info("Base de datos vacía. Iniciando poblamiento de datos demo...");

            // 1. Crear el Tenant inicial
            Tenant tenant = new Tenant();
            tenant.setName("Empresa Demo ControlPro");
            tenant.setSubdomain("miempresa");
            tenant.setStatus("ACTIVE");
            tenant = tenantRepository.save(tenant);
            
            UUID tenantId = tenant.getId();
            log.info("Tenant creado con ID: {}", tenantId);

            // 2. Establecer el contexto del Tenant para la inyección de @TenantId
            TenantContext.setCurrentTenant(tenantId);

            try {
                // 3. Crear usuario Admin
                User admin = new User();
                admin.setEmail("admin@miempresa.com");
                admin.setPassword(passwordEncoder.encode("admin123"));
                admin.setRole(Role.ADMIN_EMPRESA);
                admin.setStatus("ACTIVE");
                userRepository.save(admin);
                log.info("Usuario ADMIN creado: admin@miempresa.com / admin123");

                // 4. Crear usuario Empleado
                User employeeUser = new User();
                employeeUser.setEmail("empleado@miempresa.com");
                employeeUser.setPassword(passwordEncoder.encode("empleado123"));
                employeeUser.setRole(Role.EMPLEADO);
                employeeUser.setStatus("ACTIVE");
                employeeUser = userRepository.save(employeeUser);
                log.info("Usuario EMPLEADO creado: empleado@miempresa.com / empleado123");

                // 5. Crear la Ficha de Empleado correspondiente
                Employee employee = new Employee();
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

                // 6. Crear un Horario predeterminado
                Schedule schedule = new Schedule();
                schedule.setName("Horario Administrativo Fijo");
                schedule.setStartTime(LocalTime.of(8, 0));
                schedule.setEndTime(LocalTime.of(17, 0));
                schedule.setToleranceMinutes(10);
                schedule.setDaysOfWeek("1,2,3,4,5"); // Lunes a Viernes
                schedule = scheduleRepository.save(schedule);
                log.info("Plantilla de Horario Fijo creada");

                // 7. Asignar Horario al Empleado
                EmployeeSchedule employeeSchedule = new EmployeeSchedule();
                employeeSchedule.setEmployee(employee);
                employeeSchedule.setSchedule(schedule);
                employeeSchedule.setStartDate(LocalDate.now().minusWeeks(2));
                employeeScheduleRepository.save(employeeSchedule);
                log.info("Horario asignado al empleado");

            } finally {
                // Limpiar contexto del tenant
                TenantContext.clear();
            }

            log.info("Poblamiento de datos inicial completado.");
        } else {
            log.info("La base de datos ya contiene registros. Omitiendo poblamiento inicial.");
        }
    }
}
