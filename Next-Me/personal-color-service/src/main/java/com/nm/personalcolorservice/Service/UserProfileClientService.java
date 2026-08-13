package com.nm.personalcolorservice.Service;

import com.nm.personalcolorservice.Dto.UserPersonalColorContextDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;

import java.util.Map;

@Service
/** JWT를 그대로 전달해 user-service의 현재 사용자 프로필을 읽고 수정하는 내부 클라이언트다. */
public class UserProfileClientService {

    private final RestClient restClient;

    public UserProfileClientService(@Value("${services.user-service.url}") String userServiceUrl) {
        this.restClient = RestClient.builder().baseUrl(userServiceUrl).build();
    }

    public UserPersonalColorContextDto getContext(String authorizationHeader) {
        // 추천은 판정된 퍼스널컬러가 있어야 하므로 비어 있으면 분석 대화를 먼저 요구한다.
        JsonNode profile = getProfile(authorizationHeader);
        String personalColor = profile.path("personalColor").asText("").trim();
        if (personalColor.isBlank()) {
            throw new IllegalArgumentException(
                    "퍼스널컬러 분석 대화를 먼저 완료해주세요."
            );
        }
        return new UserPersonalColorContextDto(profile.toString(), personalColor);
    }

    public String getProfileJson(String authorizationHeader) {
        return getProfile(authorizationHeader).toString();
    }

    public void updatePersonalColor(String authorizationHeader, String personalColor) {
        // user-service 코드를 복제하지 않고 기존 PATCH /users/me를 재사용한다.
        validateAuthorization(authorizationHeader);
        try {
            restClient.patch()
                    .uri("/users/me")
                    .header("Authorization", authorizationHeader)
                    .body(Map.of("personalColor", personalColor))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException exception) {
            throw new IllegalStateException(
                    "판정된 퍼스널컬러를 user-service 프로필에 저장하지 못했습니다.", exception
            );
        }
    }

    private JsonNode getProfile(String authorizationHeader) {
        // 원래 요청의 JWT를 전달하므로 user-service도 같은 사용자를 식별한다.
        validateAuthorization(authorizationHeader);
        try {
            JsonNode profile = restClient.get()
                    .uri("/users/me")
                    .header("Authorization", authorizationHeader)
                    .retrieve()
                    .body(JsonNode.class);
            if (profile == null) {
                throw new IllegalStateException("사용자 프로필을 불러오지 못했습니다.");
            }
            return profile;
        } catch (RestClientException exception) {
            throw new IllegalStateException(
                    "user-service에서 사용자 프로필을 불러오지 못했습니다.", exception
            );
        }
    }

    private void validateAuthorization(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            throw new IllegalArgumentException("인증 토큰이 누락되었습니다.");
        }
    }
}
