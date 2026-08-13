package com.nm.personalcolorservice.Dto;

/** 오류 응답을 프론트가 공통 처리할 수 있도록 message 필드 하나로 통일한다. */
public record ErrorResponseDto(String message) {
}
