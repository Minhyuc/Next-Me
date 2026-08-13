package com.nm.routineservice.Dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
/** 오늘만 달라진 일정을 자연어로 받는 요청이다. */
public class AdjustRoutineRequestDto {

    @NotBlank(message = "변경된 일정을 입력해주세요.")
    @Size(max = 1000, message = "일정 변경 내용은 1,000자 이하로 입력해주세요.")
    private String message; // 예: "오늘은 오후 7시에 퇴근할 거야"
}
