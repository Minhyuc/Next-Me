package com.nm.personalcolorservice.Service;

import com.nm.personalcolorservice.Dto.AiPersonalColorResultDto;
import com.nm.personalcolorservice.Dto.CosmeticRecommendationDto;
import com.nm.personalcolorservice.Dto.OutfitRecommendationDto;
import com.nm.personalcolorservice.Dto.PersonalColorRecommendationRequestDto;
import com.nm.personalcolorservice.Dto.MakeupRecommendationResponseDto;
import com.nm.personalcolorservice.Dto.OutfitRecommendationResponseDto;
import com.nm.personalcolorservice.Dto.PageResponseDto;
import com.nm.personalcolorservice.Dto.UserPersonalColorContextDto;
import com.nm.personalcolorservice.Entity.MakeupRecommendation;
import com.nm.personalcolorservice.Entity.OutfitRecommendation;
import com.nm.personalcolorservice.Repository.MakeupRecommendationRepository;
import com.nm.personalcolorservice.Repository.OutfitRecommendationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Service
/** 옷 추천과 화장 추천을 독립적으로 생성·저장·조회하는 서비스다. */
public class PersonalColorRecommendationService {

    private final OutfitRecommendationRepository outfitRepository;
    private final MakeupRecommendationRepository makeupRepository;
    private final UserProfileClientService profileClient;
    private final OpenAiPersonalColorService openAiService;
    private final ObjectMapper objectMapper;
    private final ZoneId zoneId;

    public PersonalColorRecommendationService(
            OutfitRecommendationRepository outfitRepository,
            MakeupRecommendationRepository makeupRepository,
            UserProfileClientService profileClient,
            OpenAiPersonalColorService openAiService,
            ObjectMapper objectMapper,
            @Value("${personal-color.zone-id}") String zoneId
    ) {
        this.outfitRepository = outfitRepository;
        this.makeupRepository = makeupRepository;
        this.profileClient = profileClient;
        this.openAiService = openAiService;
        this.objectMapper = objectMapper;
        this.zoneId = ZoneId.of(zoneId);
    }

    @Transactional
    // user-service 프로필을 조회한 뒤 옷 전용 AI 결과를 저장한다.
    public OutfitRecommendationResponseDto createOutfits(
            Long userId,
            String authorizationHeader,
            PersonalColorRecommendationRequestDto request
    ) {
        PersonalColorRecommendationRequestDto safeRequest = safeRequest(request);
        LocalDate today = LocalDate.now(zoneId);
        UserPersonalColorContextDto context = profileClient.getContext(authorizationHeader);
        AiPersonalColorResultDto result = openAiService.recommendOutfits(context, safeRequest, today);
        OutfitRecommendation saved = outfitRepository.save(OutfitRecommendation.builder()
                .userId(userId)
                .recommendationDate(today)
                .personalColor(context.personalColor())
                .summary(result.getSummary())
                .requestJson(writeJson(safeRequest))
                .outfitsJson(writeJson(result.getOutfits()))
                .build());
        return toOutfitResponse(saved);
    }

    @Transactional
    // 옷 데이터가 섞이지 않는 화장 전용 AI 결과를 저장한다.
    public MakeupRecommendationResponseDto createMakeup(
            Long userId,
            String authorizationHeader,
            PersonalColorRecommendationRequestDto request
    ) {
        PersonalColorRecommendationRequestDto safeRequest = safeRequest(request);
        LocalDate today = LocalDate.now(zoneId);
        UserPersonalColorContextDto context = profileClient.getContext(authorizationHeader);
        AiPersonalColorResultDto result = openAiService.recommendMakeup(context, safeRequest, today);
        MakeupRecommendation saved = makeupRepository.save(MakeupRecommendation.builder()
                .userId(userId)
                .recommendationDate(today)
                .personalColor(context.personalColor())
                .summary(result.getSummary())
                .requestJson(writeJson(safeRequest))
                .cosmeticsJson(writeJson(result.getCosmetics()))
                .build());
        return toMakeupResponse(saved);
    }

    @Transactional(readOnly = true)
    // 옷 전용 테이블에서 오늘 날짜의 가장 최근 한 건을 조회한다.
    public OutfitRecommendationResponseDto getTodayOutfits(Long userId) {
        return outfitRepository
                .findFirstByUserIdAndRecommendationDateOrderByCreatedAtDesc(
                        userId, LocalDate.now(zoneId)
                )
                .map(this::toOutfitResponse)
                .orElseThrow(() -> new IllegalStateException(
                        "오늘 생성한 옷 추천이 없습니다. 먼저 옷 추천을 생성해주세요."
                ));
    }

    @Transactional(readOnly = true)
    // 화장 전용 테이블에서 오늘 날짜의 가장 최근 한 건을 조회한다.
    public MakeupRecommendationResponseDto getTodayMakeup(Long userId) {
        return makeupRepository
                .findFirstByUserIdAndRecommendationDateOrderByCreatedAtDesc(
                        userId, LocalDate.now(zoneId)
                )
                .map(this::toMakeupResponse)
                .orElseThrow(() -> new IllegalStateException(
                        "오늘 생성한 화장 추천이 없습니다. 먼저 화장 추천을 생성해주세요."
                ));
    }

    @Transactional(readOnly = true)
    public PageResponseDto<OutfitRecommendationResponseDto> getOutfitHistory(
            Long userId,
            int page,
            int size
    ) {
        return PageResponseDto.from(
                outfitRepository.findAllByUserIdOrderByCreatedAtDesc(
                        userId, PageRequest.of(page, size)
                ),
                this::toOutfitResponse
        );
    }

    @Transactional(readOnly = true)
    public PageResponseDto<MakeupRecommendationResponseDto> getMakeupHistory(
            Long userId,
            int page,
            int size
    ) {
        return PageResponseDto.from(
                makeupRepository.findAllByUserIdOrderByCreatedAtDesc(
                        userId, PageRequest.of(page, size)
                ),
                this::toMakeupResponse
        );
    }

    private OutfitRecommendationResponseDto toOutfitResponse(
            OutfitRecommendation recommendation
    ) {
        return OutfitRecommendationResponseDto.builder()
                .recommendationId(recommendation.getId())
                .recommendationDate(recommendation.getRecommendationDate())
                .personalColor(recommendation.getPersonalColor())
                .summary(recommendation.getSummary())
                .outfits(readOutfits(recommendation.getOutfitsJson()))
                .createdAt(recommendation.getCreatedAt())
                .build();
    }

    private MakeupRecommendationResponseDto toMakeupResponse(
            MakeupRecommendation recommendation
    ) {
        return MakeupRecommendationResponseDto.builder()
                .recommendationId(recommendation.getId())
                .recommendationDate(recommendation.getRecommendationDate())
                .personalColor(recommendation.getPersonalColor())
                .summary(recommendation.getSummary())
                .cosmetics(readCosmetics(recommendation.getCosmeticsJson()))
                .createdAt(recommendation.getCreatedAt())
                .build();
    }

    private PersonalColorRecommendationRequestDto safeRequest(
            PersonalColorRecommendationRequestDto request
    ) {
        // Bean Validation 외에도 서비스 직접 호출에 대비해 빈 메시지를 방어한다.
        if (request == null || request.getMessage() == null || request.getMessage().isBlank()) {
            throw new IllegalArgumentException("추천받고 싶은 내용을 입력해주세요.");
        }
        return request;
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JacksonException exception) {
            throw new IllegalStateException("퍼스널컬러 추천을 저장할 수 없습니다.", exception);
        }
    }

    private List<CosmeticRecommendationDto> readCosmetics(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return Arrays.asList(objectMapper.readValue(json, CosmeticRecommendationDto[].class));
        } catch (JacksonException exception) {
            throw new IllegalStateException("저장된 화장품 추천을 불러올 수 없습니다.", exception);
        }
    }

    private List<OutfitRecommendationDto> readOutfits(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return Arrays.asList(objectMapper.readValue(json, OutfitRecommendationDto[].class));
        } catch (JacksonException exception) {
            throw new IllegalStateException("저장된 옷 추천을 불러올 수 없습니다.", exception);
        }
    }
}
