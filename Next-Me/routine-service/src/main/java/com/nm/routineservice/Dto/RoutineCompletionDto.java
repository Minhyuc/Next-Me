package com.nm.routineservice.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
/** 하루 루틴을 100% 완료한 한 번의 기록이다. */
public class RoutineCompletionDto {
    private LocalDate completedDate; // 완료한 루틴 날짜
    private LocalDateTime completedAt; // 마지막 항목까지 완료한 정확한 시각
    private long completionNumber; // 사용자의 누적 몇 번째 완료인지 나타내는 번호
}
