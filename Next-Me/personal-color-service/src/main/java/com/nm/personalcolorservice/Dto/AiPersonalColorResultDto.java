package com.nm.personalcolorservice.Dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
/** OpenAI의 옷·화장품 추천 결과를 내부에서 공통으로 전달하는 DTO다. */
public class AiPersonalColorResultDto {
    private String summary; // 추천 전체를 설명하는 짧은 요약
    private List<CosmeticRecommendationDto> cosmetics; // 화장 요청일 때의 추천 목록
    private List<OutfitRecommendationDto> outfits; // 옷 요청일 때의 추천 목록
}
