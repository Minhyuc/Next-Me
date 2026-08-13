package com.nm.routineservice.Dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
/** 루틴 카드의 완료 체크 상태를 변경한다. */
public class UpdateRoutineItemRequestDto {

    @NotNull(message = "완료 여부를 입력해주세요.")
    private Boolean completed; // true: 완료, false: 완료 취소
}
