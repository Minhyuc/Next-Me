package com.nm.routineservice.Service;

import com.nm.routineservice.Dto.UserRoutineContextDto;
import com.nm.routineservice.Entity.RoutineCategory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.EnumSet;
import java.util.Set;

@Service
/** 루틴 AI에 필요한 프로필과 선택 카테고리를 user-service에서 가져온다. */
public class UserRoutineContextClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public UserRoutineContextClient(
            @Value("${services.user-service.url}") String userServiceUrl,
            ObjectMapper objectMapper
    ) {
        this.restClient = RestClient.builder().baseUrl(userServiceUrl).build();
        this.objectMapper = objectMapper;
    }

    public UserRoutineContextDto getContext(String authorizationHeader) {
        // 같은 JWT를 전달하여 user-service에서도 동일 사용자를 조회한다.
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            throw new IllegalArgumentException("인증 토큰이 누락되었습니다.");
        }

        try {
            String profileJson = restClient.get()
                    .uri("/users/me")
                    .header("Authorization", authorizationHeader)
                    .retrieve()
                    .body(String.class);
            String preferenceJson = restClient.get()
                    .uri("/users/me/routine-preferences")
                    .header("Authorization", authorizationHeader)
                    .retrieve()
                    .body(String.class);

            if (profileJson == null || preferenceJson == null) {
                throw new IllegalStateException("사용자 프로필 또는 루틴 선호도를 불러오지 못했습니다.");
            }
            return new UserRoutineContextDto(
                    profileJson,
                    preferenceJson,
                    extractSelectedCategories(preferenceJson)
            );
        } catch (RestClientException exception) {
            throw new IllegalStateException("user-service에서 프로필과 루틴 선호도를 불러오지 못했습니다.", exception);
        }
    }

    private Set<RoutineCategory> extractSelectedCategories(String preferenceJson) {
        // true로 선택된 항목만 enum 집합으로 변환해 AI 결과 필터에 사용한다.
        try {
            JsonNode preferences = objectMapper.readTree(preferenceJson);
            Set<RoutineCategory> selected = EnumSet.noneOf(RoutineCategory.class);
            addIfTrue(preferences, "routineCleansing", RoutineCategory.CLEANSING, selected);
            addIfTrue(preferences, "routineSkinCare", RoutineCategory.SKIN_CARE, selected);
            // 퍼스널컬러 추천은 독립 personal-color-service에서 제공한다.
            addIfTrue(preferences, "routineSleepWake", RoutineCategory.SLEEP_WAKE, selected);
            addIfTrue(preferences, "routineDiet", RoutineCategory.DIET, selected);
            addIfTrue(preferences, "routineExercise", RoutineCategory.EXERCISE, selected);
            return selected;
        } catch (JacksonException exception) {
            throw new IllegalStateException("루틴 선호도 응답을 처리할 수 없습니다.", exception);
        }
    }

    private void addIfTrue(JsonNode node, String field, RoutineCategory category,
                           Set<RoutineCategory> selected) {
        if (node.path(field).asBoolean(false)) {
            selected.add(category);
        }
    }
}
