package com.nm.aichatservice.Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;

/** JWT를 전달해 user-service에서 현재 사용자의 프로필을 조회한다. */
@Service
public class UserProfileClientService {

    private final RestClient restClient;

    public UserProfileClientService(@Value("${services.user-service.url}") String userServiceUrl) {
        // application.yml의 내부 user-service 주소를 기본 URL로 사용한다.
        this.restClient = RestClient.builder()
                .baseUrl(userServiceUrl)
                .build();
    }

    public String getProfileContext(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            return "";
        }

        try {
            // 로그인 때 받은 같은 Bearer 토큰을 전달하므로 user-service도 본인을 식별할 수 있다.
            JsonNode profile = restClient.get()
                    .uri("/users/me")
                    .header("Authorization", authorizationHeader)
                    .retrieve()
                    .body(JsonNode.class);
            return profile == null ? "" : profile.toString();
        } catch (RestClientException exception) {
            // 프로필 서비스가 일시적으로 응답하지 않아도 일반 대화는 계속 제공한다.
            return "";
        }
    }
}
