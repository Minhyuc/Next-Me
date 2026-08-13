package com.nm.routineservice.Entity;

/** 프론트가 카테고리별 아이콘·색상을 정할 때 사용하는 고정 코드다. */
public enum RoutineCategory {
    CLEANSING,      // 세안
    SKIN_CARE,      // 보습 등 스킨케어
    PERSONAL_COLOR, // 현재 루틴 생성에서는 제외하고 별도 서비스가 담당
    SLEEP_WAKE,     // 취침·기상
    DIET,           // 식사·수분 섭취
    EXERCISE        // 운동
}
