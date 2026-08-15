package com.nm.userservice.Dto;

/** 프론트가 모든 잘못된 요청을 같은 방식으로 처리할 수 있게 오류 형식을 통일한다. */
public record ErrorResponseDto(String message) {
}
