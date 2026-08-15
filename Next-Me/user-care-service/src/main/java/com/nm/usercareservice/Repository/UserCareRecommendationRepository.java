package com.nm.usercareservice.Repository;

import com.nm.usercareservice.Entity.UserCareRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserCareRecommendationRepository
        extends JpaRepository<UserCareRecommendation, Long> {

    Optional<UserCareRecommendation> findFirstByUserIdOrderByCreatedAtDesc(Long userId);
}
