package com.nm.userservice.Controller;

import com.nm.userservice.Dto.*;
import com.nm.userservice.Service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    @PostMapping("/sync")
    public ResponseEntity<UserResponseDto> sync(@RequestBody UserSyncRequestDto dto) {
        return ResponseEntity.ok(service.syncUser(dto));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponseDto> get(HttpServletRequest request) {
        Long userId = getUserId(request);
        return ResponseEntity.ok(service.getProfile(userId));
    }

    @PostMapping("/me/profile")
    public ResponseEntity<UserResponseDto> createOrUpdateProfile(
            HttpServletRequest request,
            @RequestBody UserProfileRequestDto dto
    ) {
        Long userId = getUserId(request);
        return ResponseEntity.ok(service.saveProfile(userId, dto));
    }

    @PatchMapping("/me")
    public ResponseEntity<UserResponseDto> update(
            HttpServletRequest request,
            @RequestBody UserUpdateRequestDto dto
    ) {
        Long userId = getUserId(request);
        return ResponseEntity.ok(service.updateProfile(userId, dto));
    }

    @GetMapping("/onboarding/template")
    public ResponseEntity<List<OnboardingQuestionDto>> getTemplate() {
        return ResponseEntity.ok(service.getOnboardingTemplate());
    }

    @PostMapping("/me/onboarding/answer")
    public ResponseEntity<OnboardingNextResponseDto> saveAnswer(
            HttpServletRequest request,
            @RequestBody OnboardingAnswerRequestDto dto
    ) {
        Long userId = getUserId(request);
        return ResponseEntity.ok(service.saveOnboardingAnswer(userId, dto));
    }

    // ----------------------------------------------------------------------
    // 피부 타입 세부 질문 최종 저장
    // - detailAnswers: JSON 문자열
    // - photo: optional
    // ----------------------------------------------------------------------
    @PostMapping(
            value = "/me/onboarding/skin-type-detail/final",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<OnboardingNextResponseDto> saveSkinTypeDetailFinal(
            HttpServletRequest request,
            @RequestPart("detailAnswers") String detailAnswersJson,
            @RequestPart(value = "photo", required = false) MultipartFile photo
    ) {
        Long userId = getUserId(request);
        return ResponseEntity.ok(service.saveSkinTypeDetailBulk(userId, detailAnswersJson, photo));
    }

    // ----------------------------------------------------------------------
    // 퍼스널컬러 세부 질문 최종 저장
    // - detailAnswers: JSON 문자열
    // - photo: optional
    // ----------------------------------------------------------------------
    @PostMapping(
            value = "/me/onboarding/personal-color-detail/final",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<OnboardingNextResponseDto> savePersonalColorDetailFinal(
            HttpServletRequest request,
            @RequestPart("detailAnswers") String detailAnswersJson,
            @RequestPart(value = "photo", required = false) MultipartFile photo
    ) {
        Long userId = getUserId(request);
        return ResponseEntity.ok(service.savePersonalColorDetailBulk(userId, detailAnswersJson, photo));
    }

    // ----------------------------------------------------------------------
    // 루틴 추천 항목 선택/조회
    // ----------------------------------------------------------------------
    @GetMapping("/me/routine-preferences")
    public ResponseEntity<RoutinePreferenceResponseDto> getRoutinePreferences(HttpServletRequest request) {
        Long userId = getUserId(request);
        return ResponseEntity.ok(service.getRoutinePreference(userId));
    }

    @PutMapping("/me/routine-preferences")
    public ResponseEntity<RoutinePreferenceResponseDto> saveRoutinePreferences(
            HttpServletRequest request,
            @RequestBody RoutinePreferenceRequestDto dto
    ) {
        Long userId = getUserId(request);
        return ResponseEntity.ok(service.saveRoutinePreference(userId, dto));
    }

    // =========================================================================
    // JwtAuthenticationFilter에서 저장한 userId 추출
    // =========================================================================
    private Long getUserId(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("currentUserId");
        if (userId == null) {
            throw new IllegalArgumentException("인증 토큰이 유효하지 않거나 누락되었습니다.");
        }
        return userId;
    }

    // =========================================================================
    // 사진만 재업로드
    // =========================================================================
    @PostMapping(
            value = "/me/onboarding/personal-color-detail/photo",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<OnboardingNextResponseDto> uploadPersonalColorPhoto(
            HttpServletRequest request,
            @RequestPart("photo") MultipartFile photo
    ) {
        Long userId = getUserId(request);
        return ResponseEntity.ok(
                service.reAnalyzePersonalColorPhoto(userId, photo)
        );
    }

    @PostMapping(
            value = "/me/onboarding/skin-type-detail/photo",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<OnboardingNextResponseDto> uploadSkinTypePhoto(
            HttpServletRequest request,
            @RequestPart("photo") MultipartFile photo
    ) {
        Long userId = getUserId(request);
        return ResponseEntity.ok(
                service.reAnalyzeSkinTypePhoto(userId, photo)
        );
    }
}