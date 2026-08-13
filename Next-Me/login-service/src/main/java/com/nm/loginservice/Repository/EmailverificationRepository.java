package com.nm.loginservice.Repository;

import com.nm.loginservice.Entity.Emailverification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailverificationRepository extends JpaRepository<Emailverification, Long> {
    Optional<Emailverification> findByEmail(String email);
}