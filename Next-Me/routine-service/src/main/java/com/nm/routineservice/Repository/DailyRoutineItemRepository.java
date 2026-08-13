package com.nm.routineservice.Repository;

import com.nm.routineservice.Entity.DailyRoutineItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DailyRoutineItemRepository extends JpaRepository<DailyRoutineItem, Long> {
    // itemId와 plan.userId를 함께 확인해 타인의 항목 수정을 막는다.
    Optional<DailyRoutineItem> findByIdAndPlanUserId(Long id, Long userId);
}
