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
/** 화장 추천 화면과 화장 이력 조회 화면에서 사용하는 응답이다. */
public class MakeupRecommendationResponseDto {
    private Long recommendationId; // 저장된 추천 이력 ID
    private LocalDate recommendationDate; // 추천 기준 날짜
    private String personalColor; // 추천에 사용한 퍼스널컬러
    private String summary; // 전체 화장 방향 요약
    private List<CosmeticRecommendationDto> cosmetics; // 부위별 화장품 추천
    private LocalDateTime createdAt; // 추천 생성 시각
}
