package com.nm.routineservice.Dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
/** OpenAI가 반환하는 루틴 전체 결과다. */
public class AiRoutineResultDto {
    private String coachMessage; // 생성·조정 이유를 설명하는 문구
    private String recommendedResetTime; // AI가 판단한 하루 초기화 시각 HH:mm
    private List<AiRoutineItemDto> items; // 생성된 루틴 항목
}
