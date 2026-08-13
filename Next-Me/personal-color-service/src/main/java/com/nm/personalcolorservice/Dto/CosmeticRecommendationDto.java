package com.nm.personalcolorservice.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
/** 화장 화면에 표시할 제품군 하나의 추천 상세 정보다. */
public class CosmeticRecommendationDto {
    private String category; // 립, 블러셔, 아이섀도 등 화장 부위
    private String productType; // 틴트, 팔레트 등 추천 제품 유형
    private List<String> recommendedColors; // 퍼스널컬러에 어울리는 색상 목록
    private String finish; // 매트, 글로시 등 추천 표현 방식
    private String reason; // 해당 제품과 색상을 추천한 이유
}
