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
/** 퍼스널컬러 분석 이력의 개별 사용자·AI 메시지다. */
public class PersonalColorMessageResponseDto {
    private Long messageId; // 메시지 ID
    private String role; // USER 또는 ASSISTANT
    private String content; // 사용자 답변 또는 AI 질문
    private LocalDateTime createdAt; // 메시지 생성 시각
}
