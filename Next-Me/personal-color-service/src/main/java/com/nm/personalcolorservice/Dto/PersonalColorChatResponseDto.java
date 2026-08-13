package com.nm.personalcolorservice.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
/** 퍼스널컬러 분석 대화 한 번의 AI 응답이다. */
public class PersonalColorChatResponseDto {
    private Long conversationId; // 다음 요청에 다시 사용할 분석 대화 ID
    private String reply; // 다음 질문 또는 최종 분석 설명
    private boolean completed; // 분석 완료 여부
    private String personalColor; // 완료 전에는 null, 완료 후에는 분석 결과
    private Integer confidence; // 완료 후 반환되는 신뢰도(0~100)
}
