package com.nm.personalcolorservice.Dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
/** 옷 또는 화장 추천 화면에서 사용자가 자연어로 입력하는 요청이다. */
public class PersonalColorRecommendationRequestDto {

    @NotBlank(message = "추천받고 싶은 내용을 입력해주세요.")
    @Size(max = 1000, message = "메시지는 1000자 이하로 입력해주세요.")
    private String message; // 예: "오늘 회사에 갈 건데 더우니 단정하게 추천해줘"
}
