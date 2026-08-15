package com.nm.characterservice.Repository;

import com.nm.characterservice.Entity.CharacterRewardClaim;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface CharacterRewardClaimRepository extends JpaRepository<CharacterRewardClaim, Long> {
    Optional<CharacterRewardClaim> findByUserIdAndRoutineDate(Long userId, LocalDate routineDate);
}
