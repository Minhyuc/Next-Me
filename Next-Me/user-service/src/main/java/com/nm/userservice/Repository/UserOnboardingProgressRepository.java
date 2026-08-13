package com.nm.userservice.Repository;

import com.nm.userservice.Entity.UserOnboardingProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserOnboardingProgressRepository extends JpaRepository<UserOnboardingProgress, Long> {
    Optional<UserOnboardingProgress> findByUserId(Long userId);
}
