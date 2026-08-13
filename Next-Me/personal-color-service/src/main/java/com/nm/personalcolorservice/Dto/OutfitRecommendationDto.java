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
/** 오늘의 옷 화면에 표시할 한 가지 코디 조합이다. */
public class OutfitRecommendationDto {
    private String style; // 출근룩, 캐주얼 등 전체 분위기
    private String top; // 상의 추천
    private String bottom; // 하의 추천
    private String outerwear; // 아우터 추천(필요 없으면 빈 값 가능)
    private String shoes; // 신발 추천
    private String accessories; // 가방·주얼리 등 액세서리 추천
    private List<String> colorPalette; // 함께 사용하기 좋은 색 조합
    private String reason; // 날씨·상황·퍼스널컬러를 반영한 추천 이유
}
