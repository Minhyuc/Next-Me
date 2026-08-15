package com.nm.usercareservice.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
/** 프론트 카드 한 장에 표시할 제품 유형, 성분, 이유와 사용 안내다. */
public class CareIngredientDto {
    private String careArea; // CLEANSING 또는 SKIN_CARE
    private String productType; // 예: 약산성 클렌저, 보습 크림
    private String ingredient; // 추천 또는 주의 성분
    private String reason; // 현재 프로필에 따른 선정 이유
    private String guidance; // 사용법 또는 주의 방법
}
