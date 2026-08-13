package com.nm.personalcolorservice.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
/** 오늘의 옷 추천 결과와 저장 이력을 프론트에 전달하는 응답이다. */
public class OutfitRecommendationResponseDto {
    private Long recommendationId; // 추천 이력 ID
    private LocalDate recommendationDate; // 추천 기준 날짜
    private String personalColor; // 추천에 사용한 퍼스널컬러
    private String summary; // 전체 코디 방향 요약
    private List<OutfitRecommendationDto> outfits; // AI가 제안한 코디 조합 목록
    private LocalDateTime createdAt; // 추천 생성 시각
}
