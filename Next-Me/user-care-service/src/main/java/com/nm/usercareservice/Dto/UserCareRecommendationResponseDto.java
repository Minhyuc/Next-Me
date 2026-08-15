package com.nm.usercareservice.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
/** 프론트가 추천·주의 성분 카드를 한 화면에 표시할 때 사용하는 응답이다. */
public class UserCareRecommendationResponseDto {
    private Long recommendationId;
    private String skinType;
    private String concerns;
    private String summary;
    private List<CareIngredientDto> recommendedIngredients;
    private List<CareIngredientDto> cautionIngredients;
    private String disclaimer;
    private LocalDateTime createdAt;
}
