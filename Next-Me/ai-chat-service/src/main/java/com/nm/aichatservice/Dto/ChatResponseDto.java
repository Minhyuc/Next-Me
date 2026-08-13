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
/** 메시지 전송 직후 프론트에 반환하는 AI 응답이다. */
public class ChatResponseDto {
    private Long conversationId; // 다음 메시지 요청에도 넣어 대화를 이어갈 ID
    private String answer; // 화면에 출력할 AI 답변
    private boolean outOfScope; // 허용 범위 밖 질문인지 여부
    private LocalDateTime createdAt; // AI 답변 저장 시각
}
