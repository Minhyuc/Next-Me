package com.nm.routineservice.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
/** 사용자의 하루 루틴 경계를 결정하는 현재 설정 응답이다. */
public class RoutineSettingResponseDto {
    private LocalTime resetTime; // 새 루틴 날짜가 시작되는 현지 시각
    private String zoneId; // resetTime 계산에 사용할 시간대(기본 Asia/Seoul)
}
