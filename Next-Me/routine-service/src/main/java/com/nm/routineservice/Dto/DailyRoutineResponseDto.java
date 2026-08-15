package com.nm.routineservice.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
/** 프론트가 오늘 루틴 화면 전체를 한 번에 교체할 때 사용하는 응답이다. */
public class DailyRoutineResponseDto {
    private boolean routineGenerated; // 최초 AI 루틴이 생성되었는지 여부
    private Long planId; // 오늘 계획 ID
    private LocalDate routineDate; // 현재 루틴 기준 날짜
    private LocalTime resetTime; // 다음 루틴 날짜 경계 시각
    private int completionPercentage; // 완료율 0~100
    private boolean allCompleted; // 모든 항목 완료 여부
    private LocalDateTime firstCompletedAt; // 최초 100% 달성 시각
    private CharacterRewardResponseDto characterReward; // 마지막 완료 요청에서 받은 캐릭터 보상
    private String coachMessage; // AI 안내 문구
    private List<RoutineItemResponseDto> items; // 화면에 표시할 전체 항목
}
