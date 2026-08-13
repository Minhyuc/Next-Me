package com.nm.aichatservice.Dto;

/** 모든 오류 응답의 형식을 { "message": "오류 설명" }으로 통일한다. */
public record ErrorResponseDto(String message) {
}
