package com.nm.routineservice.Dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
/** 기본 루틴 생성 시 사용자가 선택적으로 덧붙일 자연어 조건이다. */
public class GenerateRoutineRequestDto {

    @Size(max = 1000, message = "추가 요청은 1,000자 이하로 입력해주세요.")
    private String additionalRequest; // 비어 있으면 프로필과 선호도만 사용
}
