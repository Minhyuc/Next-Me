package com.nm.userservice.Repository;

import com.nm.userservice.Entity.UserRoutinePreference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRoutinePreferenceRepository extends JpaRepository<UserRoutinePreference, Long> {
    Optional<UserRoutinePreference> findByUserId(Long userId);
}