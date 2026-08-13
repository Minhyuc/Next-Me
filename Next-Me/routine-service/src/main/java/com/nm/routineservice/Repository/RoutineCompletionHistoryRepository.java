package com.nm.routineservice.Repository;

import com.nm.routineservice.Entity.RoutineCompletionHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoutineCompletionHistoryRepository extends JpaRepository<RoutineCompletionHistory, Long> {
    // 같은 계획의 100% 완료 기록이 중복 저장되지 않았는지 확인한다.
    boolean existsByPlanId(Long planId);

    long countByUserId(Long userId);

    List<RoutineCompletionHistory> findAllByUserIdOrderByCompletedAtDesc(Long userId);
}
    // 다음 completionNumber를 계산하기 위한 사용자 누적 완료 수다.
