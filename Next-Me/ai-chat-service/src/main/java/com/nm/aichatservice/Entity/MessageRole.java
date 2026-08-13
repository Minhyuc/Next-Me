package com.nm.aichatservice.Entity;

/** DB와 OpenAI 요청에서 메시지 작성자를 구분하는 값이다. */
public enum MessageRole {
    USER,      // 사용자 질문
    ASSISTANT  // AI 답변
}
