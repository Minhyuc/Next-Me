package com.nm.usercareservice.Service;

import com.nm.usercareservice.Dto.AiUserCareResultDto;
import com.nm.usercareservice.Dto.CareIngredientDto;
import com.nm.usercareservice.Dto.UserCareProfileContextDto;
import com.nm.usercareservice.Dto.UserCareRecommendationResponseDto;
import com.nm.usercareservice.Entity.UserCareRecommendation;
import com.nm.usercareservice.Exception.RecommendationNotFoundException;
import com.nm.usercareservice.Repository.UserCareRecommendationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserCareServiceTest {

    private UserCareRecommendationRepository repository;
    private UserProfileClientService profileClientService;
    private OpenAiUserCareService openAiUserCareService;
    private UserCareService userCareService;

    @BeforeEach
    void setUp() {
        repository = mock(UserCareRecommendationRepository.class);
        profileClientService = mock(UserProfileClientService.class);
        openAiUserCareService = mock(OpenAiUserCareService.class);
        userCareService = new UserCareService(
                repository,
                profileClientService,
                openAiUserCareService,
                new ObjectMapper()
        );
    }

    @Test
    void generateStoresAndReturnsExactlyThreeRecommendedAndCautionItems() {
        UserCareProfileContextDto context = new UserCareProfileContextDto(
                "{\"skinType\":\"민감성\"}", "민감성", "붉어짐"
        );
        when(profileClientService.getProfile("Bearer token")).thenReturn(context);
        when(openAiUserCareService.recommend(context)).thenReturn(validAiResult());
        when(repository.save(any(UserCareRecommendation.class))).thenAnswer(invocation -> {
            UserCareRecommendation source = invocation.getArgument(0);
            return UserCareRecommendation.builder()
                    .id(10L)
                    .userId(source.getUserId())
                    .skinType(source.getSkinType())
                    .concerns(source.getConcerns())
                    .summary(source.getSummary())
                    .recommendedIngredientsJson(source.getRecommendedIngredientsJson())
                    .cautionIngredientsJson(source.getCautionIngredientsJson())
                    .disclaimer(source.getDisclaimer())
                    .createdAt(source.getCreatedAt())
                    .build();
        });

        UserCareRecommendationResponseDto response =
                userCareService.generate(1L, "Bearer token");

        assertEquals(10L, response.getRecommendationId());
        assertEquals("민감성", response.getSkinType());
        assertEquals(3, response.getRecommendedIngredients().size());
        assertEquals(3, response.getCautionIngredients().size());
        verify(repository).save(any(UserCareRecommendation.class));
    }

    @Test
    void generateRejectsResultThatDoesNotContainThreeItems() {
        UserCareProfileContextDto context = new UserCareProfileContextDto(
                "{\"skinType\":\"건성\"}", "건성", "건조함"
        );
        when(profileClientService.getProfile("Bearer token")).thenReturn(context);
        when(openAiUserCareService.recommend(context)).thenReturn(
                new AiUserCareResultDto(
                        "요약",
                        List.of(item("CLEANSING", "세라마이드")),
                        validItems("주의"),
                        "패치 테스트를 권장합니다."
                )
        );

        assertThrows(
                IllegalStateException.class,
                () -> userCareService.generate(1L, "Bearer token")
        );
        verify(repository, never()).save(any());
    }

    @Test
    void latestThrowsNotFoundBeforeFirstGeneration() {
        when(repository.findFirstByUserIdOrderByCreatedAtDesc(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                RecommendationNotFoundException.class,
                () -> userCareService.getLatest(1L)
        );
    }

    @Test
    void latestRestoresStoredJsonForFrontend() {
        ObjectMapper objectMapper = new ObjectMapper();
        List<CareIngredientDto> recommended = validItems("추천");
        List<CareIngredientDto> caution = validItems("주의");
        UserCareRecommendation entity = UserCareRecommendation.builder()
                .id(20L)
                .userId(1L)
                .skinType("복합성")
                .concerns("피지")
                .summary("저자극 관리가 필요합니다.")
                .recommendedIngredientsJson(objectMapper.writeValueAsString(recommended))
                .cautionIngredientsJson(objectMapper.writeValueAsString(caution))
                .disclaimer("패치 테스트 후 사용하세요.")
                .createdAt(LocalDateTime.of(2026, 8, 15, 10, 0))
                .build();
        when(repository.findFirstByUserIdOrderByCreatedAtDesc(1L))
                .thenReturn(Optional.of(entity));

        UserCareRecommendationResponseDto response = userCareService.getLatest(1L);

        assertEquals(20L, response.getRecommendationId());
        assertEquals(3, response.getRecommendedIngredients().size());
        assertEquals("추천-1", response.getRecommendedIngredients().get(0).getIngredient());
    }

    private AiUserCareResultDto validAiResult() {
        return new AiUserCareResultDto(
                "민감성 피부를 위한 저자극 관리입니다.",
                validItems("추천"),
                validItems("주의"),
                "새 제품은 국소 부위에 패치 테스트 후 사용하세요."
        );
    }

    private List<CareIngredientDto> validItems(String prefix) {
        return List.of(
                item("CLEANSING", prefix + "-1"),
                item("SKIN_CARE", prefix + "-2"),
                item("SKIN_CARE", prefix + "-3")
        );
    }

    private CareIngredientDto item(String careArea, String ingredient) {
        return CareIngredientDto.builder()
                .careArea(careArea)
                .productType("제품 유형")
                .ingredient(ingredient)
                .reason("피부 프로필에 따른 이유")
                .guidance("저녁에 소량 사용하세요.")
                .build();
    }
}
