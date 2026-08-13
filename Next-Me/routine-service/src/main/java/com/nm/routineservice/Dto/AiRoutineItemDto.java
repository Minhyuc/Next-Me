package com.nm.routineservice.Dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
/** OpenAI 구조화 응답의 루틴 항목 한 건이다. 외부 API에는 직접 노출하지 않는다. */
public class AiRoutineItemDto {
    private Long originalItemId; // 조정 시 기존 항목 ID, 신규 생성이면 null
    private String category; // RoutineCategory 문자열
    private String title; // 사용자 화면 제목
    private String description; // 구체적인 수행 방법
    private String scheduledTime; // HH:mm
    private int completionWindowMinutes; // 실행 가능 제한 시간(분)
}
