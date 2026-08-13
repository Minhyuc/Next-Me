package com.nm.personalcolorservice.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
/** 퍼스널컬러 분석 대화 목록에 표시할 요약 정보다. */
public class PersonalColorConversationResponseDto {
    private Long conversationId; // 분석 대화 ID
    private String status; // 진행 중 또는 완료 상태
    private String personalColor; // 완료된 분석 결과
    private Integer confidence; // 완료된 분석 신뢰도
    private LocalDateTime updatedAt; // 마지막 대화 시각
}
