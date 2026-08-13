package com.nm.aichatservice.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
/** 대화 이력 화면에 표시할 개별 메시지 정보다. */
public class MessageResponseDto {
    private Long id; // 메시지 ID
    private String role; // USER 또는 ASSISTANT
    private String content; // 질문 또는 답변 본문
    private boolean outOfScope; // 범위 밖 질문에 대한 안내 답변인지 여부
    private LocalDateTime createdAt; // 메시지 저장 시각
}
