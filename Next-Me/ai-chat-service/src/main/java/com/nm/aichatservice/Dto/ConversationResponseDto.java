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
/** 대화방 목록 화면의 한 항목을 나타낸다. */
public class ConversationResponseDto {
    private Long id; // 대화방 ID
    private String title; // 첫 질문으로 자동 생성된 제목
    private LocalDateTime createdAt; // 대화 시작 시각
    private LocalDateTime updatedAt; // 마지막 메시지를 주고받은 시각
}
