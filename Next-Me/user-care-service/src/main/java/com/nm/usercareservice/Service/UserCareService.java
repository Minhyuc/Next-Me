package com.nm.usercareservice.Service;

import com.nm.usercareservice.Dto.AiUserCareResultDto;
import com.nm.usercareservice.Dto.CareIngredientDto;
import com.nm.usercareservice.Dto.UserCareProfileContextDto;
import com.nm.usercareservice.Dto.UserCareRecommendationResponseDto;
import com.nm.usercareservice.Entity.UserCareRecommendation;
import com.nm.usercareservice.Exception.RecommendationNotFoundException;
import com.nm.usercareservice.Repository.UserCareRecommendationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
/** 프로필 조회, AI 추천 생성, DB 저장과 최신 결과 조회를 연결한다. */
public class UserCareService {

    private final UserCareRecommendationRepository recommendationRepository;
    private final UserProfileClientService profileClientService;
    private final OpenAiUserCareService openAiUserCareService;
    private final ObjectMapper objectMapper;

    public UserCareService(
            UserCareRecommendationRepository recommendationRepository,
            UserProfileClientService profileClientService,
            OpenAiUserCareService openAiUserCareService,
            ObjectMapper objectMapper
    ) {
        this.recommendationRepository = recommendationRepository;
        this.profileClientService = profileClientService;
        this.openAiUserCareService = openAiUserCareService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public UserCareRecommendationResponseDto generate(
            Long userId,
            String authorizationHeader
    ) {
        UserCareProfileContextDto context = profileClientService.getProfile(authorizationHeader);
        AiUserCareResultDto aiResult = openAiUserCareService.recommend(context);
        validateResult(aiResult);

        UserCareRecommendation saved = recommendationRepository.save(
                UserCareRecommendation.builder()
                        .userId(userId)
                        .skinType(context.skinType())
                        .concerns(context.concerns())
                        .summary(aiResult.getSummary())
                        .recommendedIngredientsJson(serialize(aiResult.getRecommendedIngredients()))
                        .cautionIngredientsJson(serialize(aiResult.getCautionIngredients()))
                        .disclaimer(aiResult.getDisclaimer())
                        .createdAt(LocalDateTime.now())
                        .build()
        );
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public UserCareRecommendationResponseDto getLatest(Long userId) {
        UserCareRecommendation recommendation = recommendationRepository
                .findFirstByUserIdOrderByCreatedAtDesc(userId)
                .orElseThrow(() -> new RecommendationNotFoundException(
                        "아직 생성된 피부 관리 성분 추천이 없습니다."
                ));
        return toResponse(recommendation);
    }

    private void validateResult(AiUserCareResultDto result) {
        if (result.getRecommendedIngredients() == null
                || result.getRecommendedIngredients().size() != 3
                || result.getCautionIngredients() == null
                || result.getCautionIngredients().size() != 3) {
            throw new IllegalStateException("추천 성분과 주의 성분은 각각 세 가지여야 합니다.");
        }
        if (result.getSummary() == null || result.getSummary().isBlank()
                || result.getDisclaimer() == null || result.getDisclaimer().isBlank()) {
            throw new IllegalStateException("AI 피부 관리 추천 설명이 비어 있습니다.");
        }
        validateItems(result.getRecommendedIngredients());
        validateItems(result.getCautionIngredients());
    }

    private void validateItems(List<CareIngredientDto> items) {
        boolean hasCleansing = false;
        boolean hasSkinCare = false;
        for (CareIngredientDto item : items) {
            if (item.getIngredient() == null || item.getIngredient().isBlank()
                    || item.getProductType() == null || item.getProductType().isBlank()
                    || item.getReason() == null || item.getReason().isBlank()
                    || item.getGuidance() == null || item.getGuidance().isBlank()) {
                throw new IllegalStateException("AI 성분 추천 항목에 빈 내용이 있습니다.");
            }
            hasCleansing |= "CLEANSING".equals(item.getCareArea());
            hasSkinCare |= "SKIN_CARE".equals(item.getCareArea());
        }
        if (!hasCleansing || !hasSkinCare) {
            throw new IllegalStateException("세안과 스킨케어 추천이 모두 포함되어야 합니다.");
        }
    }

    private String serialize(List<CareIngredientDto> items) {
        return objectMapper.writeValueAsString(items);
    }

    private List<CareIngredientDto> deserialize(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            List<CareIngredientDto> items = new ArrayList<>();
            root.forEach(item -> items.add(CareIngredientDto.builder()
                    .careArea(item.path("careArea").asText())
                    .productType(item.path("productType").asText())
                    .ingredient(item.path("ingredient").asText())
                    .reason(item.path("reason").asText())
                    .guidance(item.path("guidance").asText())
                    .build()));
            return items;
        } catch (JacksonException exception) {
            throw new IllegalStateException("저장된 피부 관리 추천을 불러올 수 없습니다.", exception);
        }
    }

    private UserCareRecommendationResponseDto toResponse(UserCareRecommendation entity) {
        return UserCareRecommendationResponseDto.builder()
                .recommendationId(entity.getId())
                .skinType(entity.getSkinType())
                .concerns(entity.getConcerns())
                .summary(entity.getSummary())
                .recommendedIngredients(deserialize(entity.getRecommendedIngredientsJson()))
                .cautionIngredients(deserialize(entity.getCautionIngredientsJson()))
                .disclaimer(entity.getDisclaimer())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
