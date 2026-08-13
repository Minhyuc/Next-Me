package com.nm.personalcolorservice.Controller;

import com.nm.personalcolorservice.Dto.PersonalColorRecommendationRequestDto;
import com.nm.personalcolorservice.Dto.MakeupRecommendationResponseDto;
import com.nm.personalcolorservice.Dto.OutfitRecommendationResponseDto;
import com.nm.personalcolorservice.Dto.PersonalColorChatRequestDto;
import com.nm.personalcolorservice.Dto.PersonalColorChatResponseDto;
import com.nm.personalcolorservice.Dto.PersonalColorConversationResponseDto;
import com.nm.personalcolorservice.Dto.PersonalColorMessageResponseDto;
import com.nm.personalcolorservice.Dto.PageResponseDto;
import com.nm.personalcolorservice.Exception.AuthenticationRequiredException;
import com.nm.personalcolorservice.Service.PersonalColorRecommendationService;
import com.nm.personalcolorservice.Service.PersonalColorAnalysisService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 퍼스널컬러 분석 대화와 옷/화장 추천 API를 제공하는 컨트롤러다.
 * 모든 경로의 /me는 JWT에서 추출한 현재 사용자 데이터만 처리한다는 의미다.
 */
@RestController
@RequestMapping("/personal-colors/me")
public class PersonalColorController {

    private final PersonalColorRecommendationService recommendationService;
    private final PersonalColorAnalysisService analysisService;

    public PersonalColorController(
            PersonalColorRecommendationService recommendationService,
            PersonalColorAnalysisService analysisService
    ) {
        this.recommendationService = recommendationService;
        this.analysisService = analysisService;
    }

    @PostMapping("/analysis/start")
    // 새 분석 대화방을 만들고 AI의 첫 질문을 반환한다.
    public ResponseEntity<PersonalColorChatResponseDto> startAnalysis(HttpServletRequest request) {
        return ResponseEntity.ok(analysisService.start(getUserId(request)));
    }

    @PostMapping("/analysis/messages")
    // 사용자의 답을 저장하고 AI의 다음 질문 또는 최종 판정을 반환한다.
    public ResponseEntity<PersonalColorChatResponseDto> analyze(
            HttpServletRequest request,
            @Valid @RequestBody PersonalColorChatRequestDto dto
    ) {
        return ResponseEntity.ok(analysisService.chat(
                getUserId(request), request.getHeader("Authorization"), dto
        ));
    }

    @GetMapping("/analysis/conversations")
    // 대화가 많아져도 한 번에 전부 읽지 않도록 페이지 단위로 반환한다.
    public ResponseEntity<PageResponseDto<PersonalColorConversationResponseDto>> getAnalysisConversations(
            HttpServletRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        validatePagination(page, size, 100);
        return ResponseEntity.ok(analysisService.getConversations(getUserId(request), page, size));
    }

    @GetMapping("/analysis/conversations/{conversationId}/messages")
    // 특정 대화의 메시지를 최신순으로 페이지 조회한다.
    public ResponseEntity<PageResponseDto<PersonalColorMessageResponseDto>> getAnalysisMessages(
            HttpServletRequest request,
            @PathVariable Long conversationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        validatePagination(page, size, 100);
        return ResponseEntity.ok(analysisService.getMessages(
                getUserId(request), conversationId, page, size
        ));
    }

    @PostMapping("/outfits/recommendations")
    // 자연어 message와 프로필 퍼스널컬러를 사용해 옷만 추천한다.
    public ResponseEntity<OutfitRecommendationResponseDto> createOutfits(
            HttpServletRequest request,
            @Valid @RequestBody PersonalColorRecommendationRequestDto dto
    ) {
        return ResponseEntity.ok(recommendationService.createOutfits(
                getUserId(request), request.getHeader("Authorization"), dto
        ));
    }

    @GetMapping("/outfits/recommendations/today")
    // 오늘 생성한 옷 추천 중 가장 최근 결과 한 건을 반환한다.
    public ResponseEntity<OutfitRecommendationResponseDto> getTodayOutfits(
            HttpServletRequest request
    ) {
        return ResponseEntity.ok(recommendationService.getTodayOutfits(getUserId(request)));
    }

    @GetMapping("/outfits/recommendations")
    // 옷 추천 이력을 최신순으로 페이지 조회한다.
    public ResponseEntity<PageResponseDto<OutfitRecommendationResponseDto>> getOutfitHistory(
            HttpServletRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        validatePagination(page, size, 100);
        return ResponseEntity.ok(recommendationService.getOutfitHistory(
                getUserId(request), page, size
        ));
    }

    @PostMapping("/makeup/recommendations")
    // 옷 추천과 별도로 화장품만 추천하고 별도 이력으로 저장한다.
    public ResponseEntity<MakeupRecommendationResponseDto> createMakeup(
            HttpServletRequest request,
            @Valid @RequestBody PersonalColorRecommendationRequestDto dto
    ) {
        return ResponseEntity.ok(recommendationService.createMakeup(
                getUserId(request), request.getHeader("Authorization"), dto
        ));
    }

    @GetMapping("/makeup/recommendations/today")
    // 오늘 생성한 화장 추천 중 가장 최근 결과 한 건을 반환한다.
    public ResponseEntity<MakeupRecommendationResponseDto> getTodayMakeup(
            HttpServletRequest request
    ) {
        return ResponseEntity.ok(recommendationService.getTodayMakeup(getUserId(request)));
    }

    @GetMapping("/makeup/recommendations")
    // 화장 추천 이력을 최신순으로 페이지 조회한다.
    public ResponseEntity<PageResponseDto<MakeupRecommendationResponseDto>> getMakeupHistory(
            HttpServletRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        validatePagination(page, size, 100);
        return ResponseEntity.ok(recommendationService.getMakeupHistory(
                getUserId(request), page, size
        ));
    }

    private void validatePagination(int page, int size, int maxSize) {
        // 과도한 전체 조회를 막아 DB와 API 응답 크기를 안정적으로 유지한다.
        if (page < 0) {
            throw new IllegalArgumentException("page는 0 이상이어야 합니다.");
        }
        if (size < 1 || size > maxSize) {
            throw new IllegalArgumentException("size는 1 이상 " + maxSize + " 이하여야 합니다.");
        }
    }

    private Long getUserId(HttpServletRequest request) {
        // JwtAuthenticationFilter가 검증 후 request에 넣어 둔 사용자 ID를 사용한다.
        Long userId = (Long) request.getAttribute("currentUserId");
        if (userId == null) {
            throw new AuthenticationRequiredException("인증 토큰이 유효하지 않거나 누락되었습니다.");
        }
        return userId;
    }
}
