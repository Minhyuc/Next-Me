package com.nm.routineservice.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
/** 캐릭터 보상 등에 활용할 누적 완주 통계 응답이다. */
public class RoutineStatsResponseDto {
    private long totalCompletedCount; // 지금까지 하루 루틴을 모두 완료한 횟수
    private List<RoutineCompletionDto> completions; // 날짜와 시각이 포함된 완료 이력
}
