package com.nm.aichatservice.Dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
/** OpenAI의 구조화된 응답을 서비스 내부에서 사용하는 형태로 옮긴 DTO다. */
public class OpenAiResultDto {
    private boolean inScope; // 피부·운동·루틴 관련 질문이면 true
    private String answer; // 범위 안 질문일 때 생성한 답변
}
