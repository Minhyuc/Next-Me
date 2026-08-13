package com.nm.routineservice.Repository;

import com.nm.routineservice.Entity.RoutineSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** 사용자별 루틴 초기화 시각과 시간대 설정을 저장·조회한다. */
public interface RoutineSettingRepository extends JpaRepository<RoutineSetting, Long> {
    // 사용자마다 하나의 설정만 사용하므로 Optional 단건으로 조회한다.
    Optional<RoutineSetting> findByUserId(Long userId);
}
