package com.nm.routineservice.Dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
/** 사용자가 직접 루틴 초기화 시각과 시간대를 바꿀 때 사용하는 요청이다. */
public class UpdateRoutineSettingRequestDto {

    @NotBlank(message = "초기화 시각을 입력해주세요.")
    @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "초기화 시각은 HH:mm 형식이어야 합니다.")
    private String resetTime; // 24시간제 HH:mm 형식(예: 04:00)

    private String zoneId = "Asia/Seoul"; // IANA 시간대 이름, 생략 시 한국 시간
}
