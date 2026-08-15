package com.nm.characterservice.Dto;

/** 프론트와 내부 서비스가 오류 문구를 같은 방식으로 처리하도록 통일한 응답이다. */
public record ErrorResponseDto(String message) {
}
