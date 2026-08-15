package com.nm.usercareservice.Dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
/** OpenAI가 구조화해서 반환한 추천 결과를 서비스 계층에 전달한다. */
public class AiUserCareResultDto {
    private String summary;
    private List<CareIngredientDto> recommendedIngredients;
    private List<CareIngredientDto> cautionIngredients;
    private String disclaimer;
}
