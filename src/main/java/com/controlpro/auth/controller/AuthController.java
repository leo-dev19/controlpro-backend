package com.controlpro.auth.controller;

import com.controlpro.auth.dto.LoginRequest;
import com.controlpro.auth.dto.LoginResponse;
import com.controlpro.auth.dto.RegisterTenantRequest;
import com.controlpro.auth.model.Role;
import com.controlpro.auth.model.User;
import com.controlpro.auth.repository.UserRepository;
import com.controlpro.auth.security.JwtUtils;
import com.controlpro.common.tenant.TenantContext;
import com.controlpro.tenant.model.Tenant;
import com.controlpro.tenant.repository.TenantRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuthController {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        Optional<User> userOpt = userRepository.findByEmailNative(loginRequest.getEmail());

        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Credenciales incorrectas"));
        }

        User user = userOpt.get();

        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Credenciales incorrectas"));
        }

        if (!"ACTIVE".equals(user.getStatus())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Su cuenta de usuario no está activa"));
        }

        UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );

        String jwt = jwtUtils.generateJwtToken(
                authentication,
                user.getTenantId(),
                user.getRole().name()
        );

        Optional<Tenant> tenantOpt = tenantRepository.findById(user.getTenantId());
        String tenantName = tenantOpt.map(Tenant::getName).orElse("miempresa");
        String subdomain = tenantOpt.map(Tenant::getSubdomain).orElse("miempresa");

        return ResponseEntity.ok(new LoginResponse(
                jwt,
                user.getEmail(),
                user.getRole().name(),
                user.getTenantId().toString(),
                tenantName,
                subdomain
        ));
    }

    @PostMapping("/register-tenant")
    public ResponseEntity<?> registerTenant(@Valid @RequestBody RegisterTenantRequest registerRequest) {
        String subdomain = registerRequest.getSubdomain().trim().toLowerCase();
        
        // 1. Validar si el subdominio ya existe
        if (tenantRepository.existsBySubdomain(subdomain)) {
            return ResponseEntity.badRequest().body(Map.of("message", "El subdominio ya está registrado. Intente con otro."));
        }

        // 2. Validar si el correo del administrador ya existe
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            return ResponseEntity.badRequest().body(Map.of("message", "El correo electrónico ya está registrado."));
        }

        // 3. Crear el Tenant
        Tenant tenant = new Tenant();
        tenant.setName(registerRequest.getCompanyName().trim());
        tenant.setSubdomain(subdomain);
        tenant.setStatus("ACTIVE");
        tenant = tenantRepository.save(tenant);

        // 4. Crear el Usuario Administrador asociado al Tenant
        UUID adminId = java.util.UUID.randomUUID();
        userRepository.insertUserNative(
                adminId,
                tenant.getId(),
                registerRequest.getEmail().trim(),
                passwordEncoder.encode(registerRequest.getPassword()),
                Role.ADMIN_EMPRESA.name(),
                "ACTIVE"
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "message", "Empresa registrada exitosamente. Ya puede iniciar sesión.",
                "tenantId", tenant.getId().toString(),
                "subdomain", subdomain
        ));
    }
}