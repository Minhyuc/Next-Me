package com.nm.userservice.Dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RoutinePreferenceRequestDto {

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