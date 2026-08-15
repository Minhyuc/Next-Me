package com.nm.usercareservice.Dto;

/** user-service에서 가져온 피부 관련 최소 프로필 문맥이다. */
public record UserCareProfileContextDto(
        String profileJson,
        String skinType,
        String concerns
) {
}
