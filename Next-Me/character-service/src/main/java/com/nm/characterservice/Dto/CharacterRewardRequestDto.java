package com.nm.characterservice.Dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor
/** routine-service가 100% 완료 날짜의 캐릭터 보상을 요청할 때 사용하는 내부 DTO다. */
public class CharacterRewardRequestDto {

    @NotNull(message = "userId가 필요합니다.")
    private Long userId;

    @NotNull(message = "routineDate가 필요합니다.")
    private LocalDate routineDate;
}
