package com.nm.userservice.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nm.userservice.Dto.*;
import com.nm.userservice.Entity.UserOnboardingProgress;
import com.nm.userservice.Entity.UserProfile;
import com.nm.userservice.Entity.UserRoutinePreference;
import com.nm.userservice.Repository.UserOnboardingProgressRepository;
import com.nm.userservice.Repository.UserRepository;
import com.nm.userservice.Repository.UserRoutinePreferenceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserOnboardingProgressRepository progressRepository;
    private final UserRoutinePreferenceRepository routinePreferenceRepository;
    private final ObjectMapper objectMapper;
    private final AiImageAnalysisService aiImageAnalysisService;

    public UserService(UserRepository userRepository,
                       UserOnboardingProgressRepository progressRepository,
                       UserRoutinePreferenceRepository routinePreferenceRepository,
                       ObjectMapper objectMapper,
                       AiImageAnalysisService aiImageAnalysisService) {
        this.userRepository = userRepository;
        this.progressRepository = progressRepository;
        this.routinePreferenceRepository = routinePreferenceRepository;
        this.objectMapper = objectMapper;
        this.aiImageAnalysisService = aiImageAnalysisService;
    }

    // login-service에서 userId만 전달받아 기본 프로필 생성/동기화
    @Transactional
    public UserResponseDto syncUser(UserSyncRequestDto dto) {
        UserProfile profile = userRepository.findByUserId(dto.getUserId())
                .orElseGet(() -> UserProfile.builder()
                        .userId(dto.getUserId())
                        .build());

        UserOnboardingProgress progress = progressRepository.findByUserId(dto.getUserId())
                .orElseGet(() -> UserOnboardingProgress.builder()
                        .userId(dto.getUserId())
                        .currentStep("BASIC")
                        .answersJson("{}")
                        .build());

        UserRoutinePreference routinePreference = routinePreferenceRepository.findByUserId(dto.getUserId())
                .orElseGet(() -> UserRoutinePreference.builder()
                        .userId(dto.getUserId())
                        .build());

        userRepository.save(profile);
        progressRepository.save(progress);
        routinePreferenceRepository.save(routinePreference);

        return toDto(profile);
    }

    @Transactional(readOnly = true)
    public UserResponseDto getProfile(Long userId) {
        UserProfile profile = userRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("프로필이 없습니다."));
        return toDto(profile);
    }

    // 직접 입력으로 한 번에 저장
    @Transactional
    public UserResponseDto saveProfile(Long userId, UserProfileRequestDto dto) {
        UserProfile profile = userRepository.findByUserId(userId)
                .orElseGet(() -> UserProfile.builder()
                        .userId(userId)
                        .build());

        profile.setNickname(dto.getNickname());
        profile.setGender(dto.getGender());
        profile.setAgeGroup(dto.getAgeGroup());

        profile.setWorkDay(dto.getWorkDay());
        profile.setLeaveTime(dto.getLeaveTime());
        profile.setComeTime(dto.getComeTime());
        profile.setWorkStyle(dto.getWorkStyle());

        profile.setSleepHour(dto.getSleepHour());

        profile.setMakeupFrequency(dto.getMakeupFrequency());
        profile.setSunscreenFrequency(dto.getSunscreenFrequency());

        profile.setExerciseCount(dto.getExerciseCount());
        profile.setSweatAmount(dto.getSweatAmount());

        profile.setSkinType(dto.getSkinType());
        profile.setConcerns(dto.getConcerns());

        profile.setPersonalColor(dto.getPersonalColor());

        profile.setOnboardingMode(dto.getOnboardingMode());

        UserOnboardingProgress progress = progressRepository.findByUserId(userId)
                .orElseGet(() -> UserOnboardingProgress.builder()
                        .userId(userId)
                        .currentStep("BASIC")
                        .answersJson("{}")
                        .build());

        updateCompletionState(profile, progress);

        userRepository.save(profile);
        progressRepository.save(progress);

        return toDto(profile);
    }

    // 부분 수정
    @Transactional
    public UserResponseDto updateProfile(Long userId, UserUpdateRequestDto dto) {
        UserProfile profile = userRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("프로필이 없습니다."));

        if (dto.getNickname() != null) profile.setNickname(dto.getNickname());
        if (dto.getGender() != null) profile.setGender(dto.getGender());
        if (dto.getAgeGroup() != null) profile.setAgeGroup(dto.getAgeGroup());

        if (dto.getWorkDay() != null) profile.setWorkDay(dto.getWorkDay());
        if (dto.getLeaveTime() != null) profile.setLeaveTime(dto.getLeaveTime());
        if (dto.getComeTime() != null) profile.setComeTime(dto.getComeTime());
        if (dto.getWorkStyle() != null) profile.setWorkStyle(dto.getWorkStyle());

        if (dto.getSleepHour() != null) profile.setSleepHour(dto.getSleepHour());

        if (dto.getMakeupFrequency() != null) profile.setMakeupFrequency(dto.getMakeupFrequency());
        if (dto.getSunscreenFrequency() != null) profile.setSunscreenFrequency(dto.getSunscreenFrequency());

        if (dto.getExerciseCount() != null) profile.setExerciseCount(dto.getExerciseCount());
        if (dto.getSweatAmount() != null) profile.setSweatAmount(dto.getSweatAmount());

        if (dto.getSkinType() != null) profile.setSkinType(dto.getSkinType());
        if (dto.getConcerns() != null) profile.setConcerns(dto.getConcerns());
        if (dto.getPersonalColor() != null) profile.setPersonalColor(dto.getPersonalColor());

        if (dto.getOnboardingStatus() != null) profile.setOnboardingStatus(dto.getOnboardingStatus());
        if (dto.getOnboardingMode() != null) profile.setOnboardingMode(dto.getOnboardingMode());
        if (dto.getOnboardingStep() != null) profile.setOnboardingStep(dto.getOnboardingStep());

        updateCompletionState(profile, null);

        userRepository.save(profile);
        return toDto(profile);
    }

    // 온보딩 질문 템플릿 조회
    public List<OnboardingQuestionDto> getOnboardingTemplate() {
        return List.of(
                OnboardingQuestionDto.builder()
                        .field("nickname")
                        .question("닉네임을 입력해 주세요.")
                        .type("input")
                        .required(true)
                        .build(),
                OnboardingQuestionDto.builder()
                        .field("gender")
                        .question("성별을 선택해 주세요.")
                        .type("select")
                        .required(true)
                        .options(List.of("남성", "여성"))
                        .build(),
                OnboardingQuestionDto.builder()
                        .field("ageGroup")
                        .question("연령을 입력해 주세요.")
                        .type("number")
                        .required(true)
                        .build(),

                OnboardingQuestionDto.builder()
                        .field("workDay")
                        .question("주로 일 또는 외출을 하시는 요일은 어떻게 되시나요?")
                        .type("multi-select")
                        .required(true)
                        .options(List.of("월", "화", "수", "목", "금", "토", "일", "거의 외출하지 않음"))
                        .build(),
                OnboardingQuestionDto.builder()
                        .field("leaveTime")
                        .question("24시간 기준 주된 귀가 시간을 입력해주세요.")
                        .type("number")
                        .required(false)
                        .build(),
                OnboardingQuestionDto.builder()
                        .field("comeTime")
                        .question("24시간 기준 주된 출가 시간을 입력해주세요.")
                        .type("number")
                        .required(false)
                        .build(),
                OnboardingQuestionDto.builder()
                        .field("workStyle")
                        .question("근무 스타일을 선택해 주세요.")
                        .type("select")
                        .required(false)
                        .options(List.of("일반 근무", "야근 많음", "교대 근무", "학생"))
                        .build(),
                OnboardingQuestionDto.builder()
                        .field("sleepHour")
                        .question("평균 수면 시간은 몇 시간인가요?")
                        .type("number")
                        .required(false)
                        .build(),

                OnboardingQuestionDto.builder()
                        .field("makeupFrequency")
                        .question("평소 화장을 자주 하시나요?")
                        .type("select")
                        .required(false)
                        .options(List.of("매일", "주 4~6회", "주 1~3회", "거의 안 함"))
                        .build(),
                OnboardingQuestionDto.builder()
                        .field("sunscreenFrequency")
                        .question("썬크림을 자주 바르시나요?")
                        .type("select")
                        .required(false)
                        .options(List.of("매일", "자주 바름", "가끔 바름", "거의 안 바름"))
                        .build(),

                OnboardingQuestionDto.builder()
                        .field("exerciseCount")
                        .question("주당 운동 횟수는 몇 회인가요?")
                        .type("number")
                        .required(false)
                        .build(),
                OnboardingQuestionDto.builder()
                        .field("sweatAmount")
                        .question("땀이 많이 나시는 편인가요?")
                        .type("select")
                        .required(false)
                        .options(List.of("많이 나는 편", "보통", "적은 편"))
                        .build(),

                OnboardingQuestionDto.builder()
                        .field("skinType")
                        .question("본인의 피부 타입은 무엇인가요?")
                        .type("multi-select")
                        .required(true)
                        .options(List.of("건성", "지성", "복합성", "민감성", "잘 모르겠어요"))
                        .build(),
                OnboardingQuestionDto.builder()
                        .field("concerns")
                        .question("주요 피부 고민을 선택해 주세요.")
                        .type("input")
                        .required(false)
                        .options(List.of("트러블", "건조함", "모공", "탄력", "칙칙함", "기타(입력)"))
                        .build(),

                OnboardingQuestionDto.builder()
                        .field("personalColor")
                        .question("당신의 퍼스널컬러를 알고 계신가요?")
                        .type("select")
                        .required(true)
                        .options(List.of("봄 웜", "여름 쿨", "가을 웜", "겨울 쿨", "잘 모르겠어요."))
                        .build()
        );
    }

    // 온보딩 답변 저장
    @Transactional
    public OnboardingNextResponseDto saveOnboardingAnswer(Long userId, OnboardingAnswerRequestDto dto) {
        UserProfile profile = getOrCreateProfile(userId);
        UserOnboardingProgress progress = getOrCreateProgress(userId);

        Map<String, String> answers = parseAnswers(progress.getAnswersJson());

        answers.put(dto.getField(), dto.getValue());
        progress.setAnswersJson(toJson(answers));

        applyAnswerToProfile(profile, dto);

        List<OnboardingQuestionDto> nextQuestions = null;

        if ("skinType".equals(dto.getField())) {
            if (isUnknownAnswer(dto.getValue())) {
                progress.setCurrentStep("SKIN_TYPE_DETAIL");
                nextQuestions = getSkinTypeDetailQuestions();
            } else {
                profile.setSkinType(dto.getValue());
            }
        }

        if ("personalColor".equals(dto.getField())) {
            if (isUnknownAnswer(dto.getValue())) {
                progress.setCurrentStep("PERSONAL_COLOR_DETAIL");
                nextQuestions = getPersonalColorDetailQuestions();
            } else {
                profile.setPersonalColor(dto.getValue());
            }
        }

        updateCompletionState(profile, progress);

        userRepository.save(profile);
        progressRepository.save(progress);

        return OnboardingNextResponseDto.builder()
                .completed("COMPLETE".equals(progress.getCurrentStep()))
                .detectedSkinType(null)
                .detectedPersonalColor(null)
                .nextQuestions(nextQuestions)
                .profile(toDto(profile))
                .build();
    }

    // 피부 타입 세부 질문 최종 저장
    @Transactional
    public OnboardingNextResponseDto saveSkinTypeDetailBulk(
            Long userId,
            String detailAnswersJson,
            MultipartFile photo
    ) {
        UserProfile profile = getOrCreateProfile(userId);
        UserOnboardingProgress progress = getOrCreateProgress(userId);

        Map<String, String> detailAnswers = parseJsonToMap(detailAnswersJson);
        Map<String, String> answers = parseAnswers(progress.getAnswersJson());
        answers.putAll(detailAnswers);

        if (!isSkinTypeDetailComplete(answers)) {
            throw new IllegalArgumentException("피부 타입 세부 질문 4개 항목이 모두 입력되지 않았습니다.");
        }

        progress.setAnswersJson(toJson(answers));

        String predictedSkinType = predictSkinType(answers);
        PhotoAnalysisResultDto aiResult = aiImageAnalysisService.analyzeSkinTypePhoto(photo);
        Integer aiConfidence = (aiResult != null ? aiResult.getConfidence() : null);
        profile.setSkinTypeConfidence(aiConfidence);

        String aiMessage = (aiResult != null ? aiResult.getMessage() : null);

        String aiSkinType = (aiResult != null ? aiResult.getSkinType() : null);

        // 스킨타입은 사진이 애매해도 재업로드 요구하지 않음
        String finalSkinType = predictedSkinType;

        if (org.springframework.util.StringUtils.hasText(aiSkinType)) {
            finalSkinType = combinePrediction(predictedSkinType, aiSkinType);
        }

        profile.setSkinType(finalSkinType);
        updateCompletionState(profile, progress);

        userRepository.save(profile);
        progressRepository.save(progress);

        return OnboardingNextResponseDto.builder()
                .completed("COMPLETE".equals(progress.getCurrentStep()))
                .detectedSkinType(finalSkinType)
                .detectedPersonalColor(null)
                .nextQuestions(null)
                .profile(toDto(profile))
                .photoRetryRequired(false)
                .photoRetryMessage(null)
                .analysisMessage(aiMessage)
                .skinTypeConfidence(aiConfidence)
                .build();
    }

    // 퍼스널컬러 세부 질문 최종 저장
    @Transactional
    public OnboardingNextResponseDto savePersonalColorDetailBulk(
            Long userId,
            String detailAnswersJson,
            MultipartFile photo
    ) {
        UserProfile profile = getOrCreateProfile(userId);
        UserOnboardingProgress progress = getOrCreateProgress(userId);

        Map<String, String> detailAnswers = parseJsonToMap(detailAnswersJson);
        Map<String, String> answers = parseAnswers(progress.getAnswersJson());
        answers.putAll(detailAnswers);

        if (!isPersonalColorDetailComplete(answers)) {
            throw new IllegalArgumentException("퍼스널컬러 세부 질문 4개 항목이 모두 입력되지 않았습니다.");
        }

        progress.setAnswersJson(toJson(answers));

        String predictedPersonalColor = predictPersonalColor(answers);
        PhotoAnalysisResultDto aiResult = aiImageAnalysisService.analyzePersonalColorPhoto(photo);

        String aiPersonalColor = (aiResult != null ? aiResult.getPersonalColor() : null);
        String aiMessage = (aiResult != null ? aiResult.getMessage() : null);
        Integer aiConfidence = (aiResult != null ? aiResult.getConfidence() : null);
        profile.setPersonalColorConfidence(aiConfidence);

        boolean retryRequired = false;

        // 정말 사진이 unusable 하고 AI가 null을 준 경우만 재업로드 요청
        if (photo != null && !photo.isEmpty() && !org.springframework.util.StringUtils.hasText(aiPersonalColor)) {
            retryRequired = true;
        }

        if (retryRequired) {
            profile.setOnboardingStatus("INCOMPLETE");
            progress.setCurrentStep("PERSONAL_COLOR_DETAIL");

            userRepository.save(profile);
            progressRepository.save(progress);

            return OnboardingNextResponseDto.builder()
                    .completed(false)
                    .detectedSkinType(null)
                    .detectedPersonalColor(null)
                    .nextQuestions(null)
                    .profile(toDto(profile))
                    .photoRetryRequired(true)
                    .photoRetryMessage("퍼스널컬러를 판단하기 어려워요. 정면, 자연광, 무보정 사진으로 다시 업로드해 주세요.")
                    .analysisMessage(aiMessage != null ? aiMessage : "퍼스널컬러를 판단하기에 충분한 정보가 없습니다.")
                    .personalColorConfidence(aiConfidence)
                    .build();
        }

        String finalPersonalColor = predictedPersonalColor;

        // AI가 결과를 주면 우선 반영
        if (org.springframework.util.StringUtils.hasText(aiPersonalColor)) {
            finalPersonalColor = combinePrediction(predictedPersonalColor, aiPersonalColor);
        }

        profile.setPersonalColor(finalPersonalColor);
        updateCompletionState(profile, progress);

        userRepository.save(profile);
        progressRepository.save(progress);

        return OnboardingNextResponseDto.builder()
                .completed("COMPLETE".equals(progress.getCurrentStep()))
                .detectedSkinType(null)
                .detectedPersonalColor(finalPersonalColor)
                .nextQuestions(null)
                .profile(toDto(profile))
                .photoRetryRequired(false)
                .photoRetryMessage(null)
                .analysisMessage(aiMessage)
                .personalColorConfidence(aiConfidence)
                .build();
    }

    // 루틴 선호도 저장
    @Transactional
    public RoutinePreferenceResponseDto saveRoutinePreference(Long userId, RoutinePreferenceRequestDto dto) {
        UserRoutinePreference preference = routinePreferenceRepository.findByUserId(userId)
                .orElseGet(() -> UserRoutinePreference.builder()
                        .userId(userId)
                        .build());

        preference.setRoutineCleansing(Boolean.TRUE.equals(dto.getRoutineCleansing()));
        preference.setRoutineSkinCare(Boolean.TRUE.equals(dto.getRoutineSkinCare()));

        preference.setRoutinePersonalColor(Boolean.TRUE.equals(dto.getRoutinePersonalColor()));

        preference.setRoutineSleepWake(Boolean.TRUE.equals(dto.getRoutineSleepWake()));
        preference.setRoutineDiet(Boolean.TRUE.equals(dto.getRoutineDiet()));
        preference.setRoutineExercise(Boolean.TRUE.equals(dto.getRoutineExercise()));

        routinePreferenceRepository.save(preference);
        return toRoutinePreferenceDto(preference);
    }

    // 루틴 선호도 조회
    @Transactional(readOnly = true)
    public RoutinePreferenceResponseDto getRoutinePreference(Long userId) {
        UserRoutinePreference preference = routinePreferenceRepository.findByUserId(userId)
                .orElseGet(() -> UserRoutinePreference.builder()
                        .userId(userId)
                        .build());
        return toRoutinePreferenceDto(preference);
    }

    private UserProfile getOrCreateProfile(Long userId) {
        return userRepository.findByUserId(userId)
                .orElseGet(() -> UserProfile.builder()
                        .userId(userId)
                        .build());
    }

    private UserOnboardingProgress getOrCreateProgress(Long userId) {
        return progressRepository.findByUserId(userId)
                .orElseGet(() -> UserOnboardingProgress.builder()
                        .userId(userId)
                        .currentStep("BASIC")
                        .answersJson("{}")
                        .build());
    }

    private void applyAnswerToProfile(UserProfile profile, OnboardingAnswerRequestDto dto) {
        switch (dto.getField()) {
            case "nickname":
                profile.setNickname(dto.getValue());
                break;
            case "gender":
                profile.setGender(dto.getValue());
                break;
            case "ageGroup":
                profile.setAgeGroup(parseInteger(dto.getValue()));
                break;

            case "workDay":
                profile.setWorkDay(dto.getValue());
                break;
            case "leaveTime":
                profile.setLeaveTime(dto.getValue());
                break;
            case "comeTime":
                profile.setComeTime(dto.getValue());
                break;
            case "workStyle":
                profile.setWorkStyle(dto.getValue());
                break;

            case "sleepHour":
                profile.setSleepHour(parseInteger(dto.getValue()));
                break;

            case "makeupFrequency":
                profile.setMakeupFrequency(dto.getValue());
                break;
            case "sunscreenFrequency":
                profile.setSunscreenFrequency(dto.getValue());
                break;

            case "exerciseCount":
                profile.setExerciseCount(parseInteger(dto.getValue()));
                break;
            case "sweatAmount":
                profile.setSweatAmount(dto.getValue());
                break;

            case "skinType":
                if (!isUnknownAnswer(dto.getValue())) {
                    profile.setSkinType(dto.getValue());
                }
                break;
            case "personalColor":
                if (!isUnknownAnswer(dto.getValue())) {
                    profile.setPersonalColor(dto.getValue());
                }
                break;
            case "concerns":
                profile.setConcerns(dto.getValue());
                break;

            default:
                break;
        }
    }

    private List<OnboardingQuestionDto> getSkinTypeDetailQuestions() {
        return List.of(
                OnboardingQuestionDto.builder()
                        .field("afterWashTightness")
                        .question("세안 후 피부가 당기나요?")
                        .type("select")
                        .required(true)
                        .options(List.of("예", "아니오"))
                        .build(),
                OnboardingQuestionDto.builder()
                        .field("tZoneOil")
                        .question("T존이 자주 번들거리나요?")
                        .type("select")
                        .required(true)
                        .options(List.of("예", "아니오"))
                        .build(),
                OnboardingQuestionDto.builder()
                        .field("sensitivity")
                        .question("피부가 쉽게 붉어지거나 따가운가요?")
                        .type("select")
                        .required(true)
                        .options(List.of("예", "아니오"))
                        .build(),
                OnboardingQuestionDto.builder()
                        .field("acneFrequency")
                        .question("트러블이 자주 생기나요?")
                        .type("select")
                        .required(true)
                        .options(List.of("예", "아니오"))
                        .build()
        );
    }

    private List<OnboardingQuestionDto> getPersonalColorDetailQuestions() {
        return List.of(
                OnboardingQuestionDto.builder()
                        .field("veinColor")
                        .question("손목 혈관 색은 어떤 편인가요?")
                        .type("select")
                        .required(true)
                        .options(List.of("파란빛", "초록빛", "잘 모르겠어요"))
                        .build(),
                OnboardingQuestionDto.builder()
                        .field("jewelryColor")
                        .question("평소 더 잘 어울리는 악세서리 색은?")
                        .type("select")
                        .required(true)
                        .options(List.of("실버", "골드", "둘 다 잘 어울림", "잘 모르겠어요"))
                        .build(),
                OnboardingQuestionDto.builder()
                        .field("lipColor")
                        .question("어울리는 립 컬러는 어떤 계열인가요?")
                        .type("select")
                        .required(true)
                        .options(List.of("코랄", "핑크", "오렌지", "버건디", "잘 모르겠어요"))
                        .build(),
                OnboardingQuestionDto.builder()
                        .field("overallTone")
                        .question("전체적으로 잘 어울리는 톤은?")
                        .type("select")
                        .required(true)
                        .options(List.of("밝고 화사한 편", "깊고 차분한 편", "중간", "잘 모르겠어요"))
                        .build()
        );
    }

    private boolean isSkinTypeDetailComplete(Map<String, String> answers) {
        return answers.containsKey("afterWashTightness")
                && answers.containsKey("tZoneOil")
                && answers.containsKey("sensitivity")
                && answers.containsKey("acneFrequency");
    }

    private boolean isPersonalColorDetailComplete(Map<String, String> answers) {
        return answers.containsKey("veinColor")
                && answers.containsKey("jewelryColor")
                && answers.containsKey("lipColor")
                && answers.containsKey("overallTone");
    }

    private String predictSkinType(Map<String, String> answers) {
        int dryScore = 0;
        int oilyScore = 0;
        int sensitiveScore = 0;

        if ("예".equals(answers.get("afterWashTightness"))) dryScore += 2;
        if ("예".equals(answers.get("cheekDryness"))) dryScore += 2;
        if ("예".equals(answers.get("tZoneOil"))) oilyScore += 2;
        if ("예".equals(answers.get("acneFrequency"))) oilyScore += 1;
        if ("예".equals(answers.get("sensitivity"))) sensitiveScore += 2;

        String baseType;
        if (dryScore >= 2 && oilyScore >= 2) {
            baseType = "복합성";
        } else if (oilyScore > dryScore) {
            baseType = "지성";
        } else if (dryScore > oilyScore) {
            baseType = "건성";
        } else {
            baseType = "중성";
        }

        if (sensitiveScore >= 2) {
            return baseType + ", 민감성";
        }

        return baseType;
    }

    private String predictPersonalColor(Map<String, String> answers) {
        int warmScore = 0;
        int coolScore = 0;
        int brightScore = 0;
        int deepScore = 0;

        if ("초록빛".equals(answers.get("veinColor"))) warmScore += 2;
        if ("파란빛".equals(answers.get("veinColor"))) coolScore += 2;

        if ("골드".equals(answers.get("jewelryColor"))) warmScore += 2;
        if ("실버".equals(answers.get("jewelryColor"))) coolScore += 2;
        if ("둘 다 잘 어울림".equals(answers.get("jewelryColor"))) {
            warmScore += 1;
            coolScore += 1;
        }

        if ("코랄".equals(answers.get("lipColor"))) warmScore += 2;
        if ("오렌지".equals(answers.get("lipColor"))) warmScore += 2;
        if ("핑크".equals(answers.get("lipColor"))) coolScore += 2;
        if ("버건디".equals(answers.get("lipColor"))) deepScore += 2;

        if ("밝고 화사한 편".equals(answers.get("overallTone"))) brightScore += 2;
        if ("깊고 차분한 편".equals(answers.get("overallTone"))) deepScore += 2;

        if ("잘 붉어진다".equals(answers.get("sunReaction"))) {
            coolScore += 1;
            brightScore += 2;
        }
        if ("잘 탄다".equals(answers.get("sunReaction"))) {
            warmScore += 1;
            deepScore += 2;
        }

        if (warmScore >= coolScore) {
            return brightScore >= deepScore ? "봄 웜" : "가을 웜";
        } else {
            return brightScore >= deepScore ? "여름 쿨" : "겨울 쿨";
        }
    }

    private boolean isProfileComplete(UserProfile profile) {
        return profile.getNickname() != null
                && profile.getGender() != null
                && profile.getAgeGroup() != null

                && profile.getWorkDay() != null
                && profile.getLeaveTime() != null
                && profile.getComeTime() != null
                && profile.getWorkStyle() != null

                && profile.getSleepHour() != null

                && profile.getMakeupFrequency() != null
                && profile.getSunscreenFrequency() != null

                && profile.getExerciseCount() != null
                && profile.getSweatAmount() != null

                && profile.getSkinType() != null
                && profile.getPersonalColor() != null
                && profile.getConcerns() != null;
    }

    private void updateCompletionState(UserProfile profile, UserOnboardingProgress progress) {
        if (isProfileComplete(profile)) {
            profile.setOnboardingStatus("COMPLETE");
            if (progress != null) {
                progress.setCurrentStep("COMPLETE");
            }
        } else {
            profile.setOnboardingStatus("INCOMPLETE");
        }
    }

    private boolean isUnknownAnswer(String value) {
        return "잘 모르겠어요".equals(value) || "잘 모르겠어요.".equals(value);
    }

    @Transactional
    public OnboardingNextResponseDto reAnalyzePersonalColorPhoto(
            Long userId,
            MultipartFile photo
    ) {

        UserProfile profile = getOrCreateProfile(userId);
        UserOnboardingProgress progress = getOrCreateProgress(userId);

        PhotoAnalysisResultDto aiResult =
                aiImageAnalysisService.analyzePersonalColorPhoto(photo);

        String aiColor = aiResult.getPersonalColor();
        String aiMessage = aiResult.getMessage();
        Integer aiConfidence = aiResult.getConfidence();

        boolean retryRequired = false;

        if (!org.springframework.util.StringUtils.hasText(aiColor)) {
            retryRequired = true;
        }

        if (!retryRequired) {
            profile.setPersonalColor(aiColor);
        }

        profile.setPersonalColorConfidence(aiConfidence);

        userRepository.save(profile);

        return OnboardingNextResponseDto.builder()
                .completed("COMPLETE".equals(progress.getCurrentStep()))
                .detectedSkinType(null)
                .detectedPersonalColor(aiColor)
                .nextQuestions(null)
                .photoRetryRequired(retryRequired)
                .photoRetryMessage(
                        retryRequired ?
                                "퍼스널컬러를 판단하기 어려워요. 자연광 정면 사진으로 다시 촬영해주세요."
                                : null
                )
                .analysisMessage(aiMessage)
                .personalColorConfidence(aiConfidence)
                .build();
    }

    @Transactional
    public OnboardingNextResponseDto reAnalyzeSkinTypePhoto(
            Long userId,
            MultipartFile photo
    ) {

        UserProfile profile = getOrCreateProfile(userId);
        UserOnboardingProgress progress = getOrCreateProgress(userId);

        PhotoAnalysisResultDto aiResult =
                aiImageAnalysisService.analyzeSkinTypePhoto(photo);

        String aiSkin = aiResult.getSkinType();
        String message = aiResult.getMessage();
        Integer aiConfidence = aiResult.getConfidence();

        if (org.springframework.util.StringUtils.hasText(aiSkin)) {
            profile.setSkinType(aiSkin);
        }

        profile.setSkinTypeConfidence(aiConfidence);

        userRepository.save(profile);

        return OnboardingNextResponseDto.builder()
                .completed("COMPLETE".equals(progress.getCurrentStep()))
                .detectedSkinType(aiSkin)
                .detectedPersonalColor(null)
                .nextQuestions(null)
                .photoRetryRequired(false)
                .photoRetryMessage(null)
                .analysisMessage(message)
                .skinTypeConfidence(aiConfidence)
                .build();
    }

    private Integer parseInteger(String value) {
        try {
            return value == null ? null : Integer.valueOf(value);
        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, String> parseAnswers(String json) {
        try {
            if (json == null || json.isBlank()) {
                return new HashMap<>();
            }
            return objectMapper.readValue(json, new TypeReference<Map<String, String>>() {});
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    private Map<String, String> parseJsonToMap(String json) {
        try {
            if (json == null || json.isBlank()) {
                return new HashMap<>();
            }
            return objectMapper.readValue(json, new TypeReference<Map<String, String>>() {});
        } catch (Exception e) {
            throw new IllegalArgumentException("detailAnswers JSON 형식이 올바르지 않습니다.");
        }
    }

    private String toJson(Map<String, String> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            return "{}";
        }
    }

    private String combinePrediction(String scoreResult, String aiResult) {
        if (aiResult == null || aiResult.isBlank()) {
            return scoreResult;
        }
        if (scoreResult == null || scoreResult.isBlank()) {
            return aiResult;
        }
        if (scoreResult.equals(aiResult)) {
            return scoreResult;
        }
        if (scoreResult.contains(aiResult)) {
            return scoreResult;
        }
        if (aiResult.contains(scoreResult)) {
            return aiResult;
        }
        return aiResult;
    }

    private UserResponseDto toDto(UserProfile profile) {
        return UserResponseDto.builder()
                .userId(profile.getUserId())
                .nickname(profile.getNickname())
                .gender(profile.getGender())
                .ageGroup(profile.getAgeGroup())

                .sleepHour(profile.getSleepHour())

                .workDay(profile.getWorkDay())
                .leaveTime(profile.getLeaveTime())
                .comeTime(profile.getComeTime())
                .workStyle(profile.getWorkStyle())

                .makeupFrequency(profile.getMakeupFrequency())
                .sunscreenFrequency(profile.getSunscreenFrequency())

                .exerciseCount(profile.getExerciseCount())
                .sweatAmount(profile.getSweatAmount())

                .skinType(profile.getSkinType())
                .concerns(profile.getConcerns())
                .personalColor(profile.getPersonalColor())

                .skinTypeConfidence(profile.getSkinTypeConfidence())
                .personalColorConfidence(profile.getPersonalColorConfidence())
                .build();
    }

    private RoutinePreferenceResponseDto toRoutinePreferenceDto(UserRoutinePreference preference) {
        return RoutinePreferenceResponseDto.builder()
                .userId(preference.getUserId())
                .routineCleansing(preference.getRoutineCleansing())
                .routineSkinCare(preference.getRoutineSkinCare())
                .routinePersonalColor(preference.getRoutinePersonalColor())
                .routineSleepWake(preference.getRoutineSleepWake())
                .routineDiet(preference.getRoutineDiet())
                .routineExercise(preference.getRoutineExercise())
                .build();
    }
}