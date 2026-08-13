package com.nm.personalcolorservice.Dto;

/** user-service에서 가져온 원본 프로필과 퍼스널컬러를 함께 전달한다. */
public record UserPersonalColorContextDto(
        String profileJson,   // AI가 참고할 전체 프로필 JSON
        String personalColor  // 프로필에 저장된 퍼스널컬러 값
) {
}
