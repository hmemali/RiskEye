package com.org.pfe.authen_service.repository;

import com.org.pfe.authen_service.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import com.org.pfe.authen_service.dto.CreateUserRequest;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmail(String email);

    boolean existsByEmailIgnoreCase(String email);
}