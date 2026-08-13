package com.nm.routineservice.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
/** 프론트 루틴 카드 한 개에 필요한 모든 상태를 담는다. */
public class RoutineItemResponseDto {
    private Long id; // 완료 PATCH에 넣을 itemId
    private String category; // 카드 분류 코드
    private String title; // 카드 제목
    private String description; // 수행 방법
    private LocalTime scheduledTime; // 권장 시작 시각
    private int completionWindowMinutes; // 완료 가능 제한 시간(분)
    private LocalDateTime availableAt; // 완료 버튼 활성화 시각
    private LocalDateTime deadlineAt; // 완료 버튼 마감 시각
    private int sortOrder; // 작은 값부터 표시
    private boolean completed; // 완료 여부
    private LocalDateTime completedAt; // 미완료면 null
    private boolean expired; // 마감 실패 확정 여부
    private boolean editable; // 완료 상태 변경 가능 여부
}
