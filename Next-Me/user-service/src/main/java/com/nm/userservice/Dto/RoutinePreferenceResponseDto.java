package com.nm.userservice.Dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoutinePreferenceResponseDto {
    private Long userId;

    // 피부 미용 차트
    private Boolean routineCleansing;
    private Boolean routineSkinCare;

    // 퍼스널 컬러 차트
    private Boolean routinePersonalColor;

    // 기타 차트
    private Boolean routineSleepWake;
    private Boolean routineDiet;
    private Boolean routineExercise;
}