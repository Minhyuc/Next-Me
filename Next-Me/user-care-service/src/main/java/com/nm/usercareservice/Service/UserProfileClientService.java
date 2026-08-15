package com.nm.usercareservice.Service;

import com.nm.usercareservice.Dto.UserCareProfileContextDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
/** 같은 JWT로 user-service에서 현재 사용자의 피부 관련 프로필만 가져온다. */
public class UserProfileClientService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public UserProfileClientService(
            @Value("${services.user-service.url}") String userServiceUrl,
            ObjectMapper objectMapper
    ) {
        this.restClient = RestClient.builder().baseUrl(userServiceUrl).build();
        this.objectMapper = objectMapper;
    }

    public UserCareProfileContextDto getProfile(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            throw new IllegalArgumentException("인증 토큰이 누락되었습니다.");
        }

        JsonNode profile;
        try {
            profile = restClient.get()
                    .uri("/users/me")
                    .header("Authorization", authorizationHeader)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException exception) {
            throw new IllegalStateException("user-service에서 프로필을 불러오지 못했습니다.", exception);
        }

        if (profile == null || profile.path("profileEmpty").asBoolean(true)
                || !profile.path("profileCompleted").asBoolean(false)) {
            throw new IllegalArgumentException("피부 관리 추천 전에 사용자 프로필을 먼저 완성해주세요.");
        }

        String skinType = profile.path("skinType").asText("").trim();
        String concerns = profile.path("concerns").asText("").trim();
        if (skinType.isBlank()) {
            throw new IllegalArgumentException("프로필에 피부 타입을 먼저 입력해주세요.");
        }

        // AI에는 추천에 필요한 최소 필드만 전달해 불필요한 개인정보 사용을 줄인다.
        Map<String, Object> careProfile = new LinkedHashMap<>();
        putIfPresent(careProfile, "ageGroup", profile.get("ageGroup"));
        careProfile.put("skinType", skinType);
        careProfile.put("concerns", concerns);
        putIfPresent(careProfile, "makeupFrequency", profile.get("makeupFrequency"));
        putIfPresent(careProfile, "sunscreenFrequency", profile.get("sunscreenFrequency"));
        putIfPresent(careProfile, "sweatAmount", profile.get("sweatAmount"));

        return new UserCareProfileContextDto(
                objectMapper.writeValueAsString(careProfile),
                skinType,
                concerns
        );
    }

    private void putIfPresent(Map<String, Object> target, String field, JsonNode value) {
        if (value != null && !value.isNull() && !value.asText("").isBlank()) {
            target.put(field, value.isNumber() ? value.asInt() : value.asText());
        }
    }
}
