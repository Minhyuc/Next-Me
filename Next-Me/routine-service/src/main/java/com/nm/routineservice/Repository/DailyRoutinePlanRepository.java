package com.nm.routineservice.Repository;

import com.nm.routineservice.Entity.DailyRoutinePlan;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface DailyRoutinePlanRepository extends JpaRepository<DailyRoutinePlan, Long> {

    @EntityGraph(attributePaths = "items")
    // 계획과 항목을 함께 읽어 지연 로딩 반복 쿼리를 줄인다.
    Optional<DailyRoutinePlan> findByUserIdAndRoutineDate(Long userId, LocalDate routineDate);

    @EntityGraph(attributePaths = "items")
    // 새 routineDate로 넘길 때 가장 최근 기본 스냅샷을 가진 계획을 찾는다.
    Optional<DailyRoutinePlan> findFirstByUserIdOrderByRoutineDateDesc(Long userId);
}
