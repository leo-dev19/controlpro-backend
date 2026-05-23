package com.controlpro.auth.repository;

import com.controlpro.auth.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    
    @Query(value = "SELECT * FROM users WHERE email = :email LIMIT 1", nativeQuery = true)
    Optional<User> findByEmailNative(@Param("email") String email);

    boolean existsByEmail(String email);

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO users (id, tenant_id, email, password, role, status, created_at, updated_at) " +
                   "VALUES (:id, :tenantId, :email, :password, :role, :status, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)", 
           nativeQuery = true)
    void insertUserNative(@Param("id") UUID id,
                          @Param("tenantId") UUID tenantId,
                          @Param("email") String email,
                          @Param("password") String password,
                          @Param("role") String role,
                          @Param("status") String status);
}

