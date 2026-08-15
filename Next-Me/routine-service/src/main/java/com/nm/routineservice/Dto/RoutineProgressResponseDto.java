package com.nm.routineservice.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
/** 진행바 갱신에 필요한 최소 정보만 제공한다. */
public class RoutineProgressResponseDto {

    private boolean routineGenerated; // 최초 AI 루틴 생성 여부
    private Long planId;
    private LocalDate routineDate;
    private int completedCount; // 완료 항목 수
    private int totalCount; // 전체 항목 수
    private int completionPercentage; // 완료율 0~100
    private boolean allCompleted; // 100% 여부
}
