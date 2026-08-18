package com.nm.routineservice.Dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
/** 이전 클라이언트와의 요청 호환을 위해 유지하며 기본 루틴 생성에는 반영하지 않는다. */
public class GenerateRoutineRequestDto {

    @Size(max = 1000, message = "추가 요청은 1,000자 이하로 입력해주세요.")
    private String additionalRequest; // 일회성 일정은 /today/adjust에서만 처리
}
