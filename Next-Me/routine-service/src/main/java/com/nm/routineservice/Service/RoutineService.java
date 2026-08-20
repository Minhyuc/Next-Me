package com.nm.routineservice.Service;

import com.nm.routineservice.Dto.AdjustRoutineRequestDto;
import com.nm.routineservice.Dto.AiRoutineItemDto;
import com.nm.routineservice.Dto.AiRoutineResultDto;
import com.nm.routineservice.Dto.CharacterRewardResponseDto;
import com.nm.routineservice.Dto.DailyRoutineResponseDto;
import com.nm.routineservice.Dto.GenerateRoutineRequestDto;
import com.nm.routineservice.Dto.RoutineCompletionDto;
import com.nm.routineservice.Dto.RoutineItemResponseDto;
import com.nm.routineservice.Dto.RoutineProgressResponseDto;
import com.nm.routineservice.Dto.RoutineSettingResponseDto;
import com.nm.routineservice.Dto.RoutineStatsResponseDto;
import com.nm.routineservice.Dto.UpdateRoutineSettingRequestDto;
import com.nm.routineservice.Dto.UserRoutineContextDto;
import com.nm.routineservice.Entity.DailyRoutineItem;
import com.nm.routineservice.Entity.DailyRoutinePlan;
import com.nm.routineservice.Entity.RoutineCategory;
import com.nm.routineservice.Entity.RoutineCompletionHistory;
import com.nm.routineservice.Entity.RoutineSetting;
import com.nm.routineservice.Repository.DailyRoutineItemRepository;
import com.nm.routineservice.Repository.DailyRoutinePlanRepository;
import com.nm.routineservice.Repository.RoutineCompletionHistoryRepository;
import com.nm.routineservice.Repository.RoutineSettingRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

@Service
/** 루틴의 생성부터 일일 초기화, 시간 제한, 완료율, 완료 이력까지 관리하는 핵심 서비스다. */
public class RoutineService {

    private static final int MAX_ROUTINE_ITEMS = 10;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final Pattern EMBEDDED_TIME_PATTERN = Pattern.compile(
            "(?<!\\d)([01]?\\d|2[0-3]):([0-5]\\d)(?::[0-5]\\d)?"
    );
    private static final Pattern KOREAN_TIME_PATTERN = Pattern.compile(
            "(오전|오후)?\\s*(\\d{1,2})\\s*시(?:\\s*(\\d{1,2})\\s*분)?"
    );

    private final RoutineSettingRepository settingRepository;
    private final DailyRoutinePlanRepository planRepository;
    private final DailyRoutineItemRepository itemRepository;
    private final RoutineCompletionHistoryRepository historyRepository;
    private final UserRoutineContextClient contextClient;
    private final OpenAiRoutineService openAiRoutineService;
    private final CharacterRewardClient characterRewardClient;
    private final ObjectMapper objectMapper;
    private final LocalTime defaultResetTime;
    private final String defaultZoneId;

    public RoutineService(
            RoutineSettingRepository settingRepository,
            DailyRoutinePlanRepository planRepository,
            DailyRoutineItemRepository itemRepository,
            RoutineCompletionHistoryRepository historyRepository,
            UserRoutineContextClient contextClient,
            OpenAiRoutineService openAiRoutineService,
            CharacterRewardClient characterRewardClient,
            ObjectMapper objectMapper,
            @Value("${routine.default-reset-time}") String defaultResetTime,
            @Value("${routine.default-zone-id}") String defaultZoneId
    ) {
        this.settingRepository = settingRepository;
        this.planRepository = planRepository;
        this.itemRepository = itemRepository;
        this.historyRepository = historyRepository;
        this.contextClient = contextClient;
        this.openAiRoutineService = openAiRoutineService;
        this.characterRewardClient = characterRewardClient;
        this.objectMapper = objectMapper;
        this.defaultResetTime = LocalTime.parse(defaultResetTime, TIME_FORMATTER);
        this.defaultZoneId = defaultZoneId;
    }

    @Transactional
    // 일정 조정된 현재 items를 재사용하지 않고 프로필과 선택 범주만으로 기본 루틴을 생성한다.
    public DailyRoutineResponseDto generateToday(
            Long userId,
            String authorizationHeader,
            GenerateRoutineRequestDto request
    ) {
        // user-service에서 현재 프로필과 선택한 루틴 카테고리를 가져온다.
        UserRoutineContextDto context = contextClient.getContext(authorizationHeader);
        validateSelectedCategories(context.getSelectedCategories());

        RoutineSetting setting = getOrCreateSetting(userId);
        LocalDate previousRoutineDate = calculateRoutineDate(setting);
        Optional<DailyRoutinePlan> previousPlan = planRepository.findByUserIdAndRoutineDate(
                userId, previousRoutineDate
        );
        previousPlan.ifPresent(this::refreshExpiredLocks);
        List<DailyRoutineItem> previousItems = previousPlan
                .map(existing -> new ArrayList<>(existing.getItems()))
                .orElseGet(ArrayList::new);

        ZonedDateTime currentDateTime = ZonedDateTime.now(ZoneId.of(setting.getZoneId()));
        AiRoutineResultDto aiResult = openAiRoutineService.generate(
                context,
                // 기본 생성에는 일회성 자연어 일정 변경을 전달하지 않는다.
                null,
                currentDateTime
        );

        // AI가 생활 패턴을 보고 제안한 초기화 시각을 사용자 설정으로 저장한다.
        LocalTime aiResetTime = parseAiResetTime(aiResult.getRecommendedResetTime());
        setting.update(aiResetTime, setting.getZoneId());
        settingRepository.save(setting);
        LocalDate routineDate = calculateRoutineDate(setting);

        DailyRoutinePlan plan = planRepository.findByUserIdAndRoutineDate(userId, routineDate)
                .orElseGet(() -> DailyRoutinePlan.builder()
                        .userId(userId)
                        .routineDate(routineDate)
                        .resetTimeSnapshot(setting.getResetTime())
                        .build());
        if (previousItems.isEmpty() && !plan.getItems().isEmpty()) {
            previousItems = new ArrayList<>(plan.getItems());
        }

        List<DailyRoutineItem> generatedItems = toNewItems(
                aiResult.getItems(), context.getSelectedCategories(), Map.of()
        );
        generatedItems = sortAndReindexFromReset(generatedItems, aiResetTime);
        int persistentCount = determinePersistentRoutineCount(
                previousItems.size(), generatedItems.size(), context.getSelectedCategories()
        );
        List<DailyRoutineItem> baselineItems = selectPersistentBaselineItems(
                generatedItems, persistentCount, context.getSelectedCategories()
        );
        baselineItems = sortAndReindexFromReset(baselineItems, aiResetTime);

        plan.updateResetTimeSnapshot(aiResetTime);
        RegenerationMergeResult regeneration = previousItems.isEmpty()
                ? new RegenerationMergeResult(baselineItems, 0, baselineItems.size())
                : mergeRegeneratedItems(
                        previousPlan.orElse(plan),
                        plan,
                        previousItems,
                        baselineItems,
                        context.getSelectedCategories(),
                        currentDateTime.toLocalDateTime()
                );
        List<DailyRoutineItem> appliedItems = sortAndReindexFromReset(
                regeneration.items(), aiResetTime
        );

        // 기본 루틴은 계속 유지한다. 특정 날짜의 완료 상태나 일정 조정 결과는 스냅샷에 섞지 않는다.
        plan.updateBaselineItemsJson(serializeBaselineItems(baselineItems));
        plan.replaceItems(appliedItems);
        plan.updateAdjustmentSummary(aiResult.getCoachMessage()
                + " 기본 루틴 " + baselineItems.size() + "개를 저장했어요. 오늘 이미 시작된 "
                + regeneration.preservedPastCount() + "개는 유지하고, 이후 "
                + regeneration.replacedFutureCount() + "개를 즉시 반영했어요.");
        plan.updateProgress();
        planRepository.save(plan);
        return toResponse(plan);
    }

    @Transactional
    // 조회할 때도 마감 시간이 지난 미완료 항목을 영구 실패 상태로 갱신한다.
    public DailyRoutineResponseDto getToday(Long userId) {
        RoutineSetting setting = getOrCreateSetting(userId);
        return findOrRollOverTodayPlan(userId, setting)
                .map(this::toResponse)
                .orElseGet(() -> toEmptyResponse(setting));
    }

    @Transactional
    // 완료 변경 후 계획의 퍼센트와 최초 100% 완료 이력을 함께 갱신한다.
    public RoutineProgressResponseDto getTodayProgress(Long userId) {
        RoutineSetting setting = getOrCreateSetting(userId);
        Optional<DailyRoutinePlan> optionalPlan = findOrRollOverTodayPlan(userId, setting);
        if (optionalPlan.isEmpty()) {
            return RoutineProgressResponseDto.builder()
                    .routineGenerated(false)
                    .routineDate(calculateRoutineDate(setting))
                    .completedCount(0)
                    .totalCount(0)
                    .completionPercentage(0)
                    .allCompleted(false)
                    .build();
        }

        DailyRoutinePlan plan = optionalPlan.get();
        refreshExpiredLocks(plan);
        int totalCount = plan.getItems().size();
        int completedCount = (int) plan.getItems().stream()
                .filter(DailyRoutineItem::isCompleted)
                .count();

        return RoutineProgressResponseDto.builder()
                .routineGenerated(true)
                .planId(plan.getId())
                .routineDate(plan.getRoutineDate())
                .completedCount(completedCount)
                .totalCount(totalCount)
                .completionPercentage(plan.getCompletionPercentage())
                .allCompleted(totalCount > 0 && completedCount == totalCount)
                .build();
    }

    @Transactional
    // AI 조정 결과에서도 완료/마감 실패 상태는 잃지 않도록 기존 ID와 대응한다.
    public DailyRoutineResponseDto updateItem(Long userId, Long itemId, boolean completed) {
        DailyRoutinePlan today = getOrRollOverTodayPlan(userId);
        refreshExpiredLocks(today);
        DailyRoutineItem item = itemRepository.findByIdAndPlanUserId(itemId, userId)
                .orElseThrow(() -> new IllegalArgumentException("루틴 항목을 찾을 수 없습니다."));

        if (!item.getPlan().getId().equals(today.getId())) {
            throw new IllegalArgumentException("오늘의 루틴 항목만 변경할 수 있습니다.");
        }

        validateItemEditable(today, item);

        item.changeCompleted(completed);
        itemRepository.save(item);
        today.updateProgress();
        planRepository.save(today);
        recordCompletionOnce(today);
        return toResponse(today, rewardCharacterIfCompleted(today));
    }

    @Transactional
    public DailyRoutineResponseDto adjustToday(
            Long userId,
            String authorizationHeader,
            AdjustRoutineRequestDto request
    ) {
        DailyRoutinePlan plan = getOrRollOverTodayPlan(userId);
        refreshExpiredLocks(plan);
        List<DailyRoutineItem> before = new ArrayList<>(plan.getItems());
        List<DailyRoutineItem> adjustableItems = before.stream()
                .filter(item -> !item.isCompleted() && !item.isMissed())
                .toList();
        if (adjustableItems.isEmpty()) {
            plan.updateAdjustmentSummary(
                    "변경 가능한 루틴이 없습니다. 이미 완료되었거나 마감된 항목은 수정할 수 없습니다."
            );
            planRepository.save(plan);
            return toResponse(plan);
        }

        UserRoutineContextDto context = contextClient.getContext(authorizationHeader);
        validateSelectedCategories(context.getSelectedCategories());
        AiRoutineResultDto result = openAiRoutineService.adjust(
                context, adjustableItems, plan.getRoutineDate(), request.getMessage()
        );

        // AI는 변경이 필요한 일부 항목만 반환할 수 있다. 응답에서 빠진 항목은 기존 값 그대로 유지한다.
        AdjustmentMergeResult mergeResult = mergeAdjustedItems(
                result.getItems(), context.getSelectedCategories(), before
        );
        if (mergeResult.appliedChanges() == 0) {
            boolean aiReturnedNoAffectedItems = result.getItems() == null || result.getItems().isEmpty();
            String aiExplanation = result.getCoachMessage();
            String noChangeSummary = aiReturnedNoAffectedItems
                    && aiExplanation != null
                    && !aiExplanation.isBlank()
                    ? aiExplanation.trim()
                    : "입력한 일정을 확인했지만 실제로 변경할 수 있는 루틴이 없습니다. 완료되었거나 마감된 항목은 유지됩니다.";
            plan.updateAdjustmentSummary(noChangeSummary);
            planRepository.save(plan);
            return toResponse(plan);
        }

        List<DailyRoutineItem> orderedAdjusted = sortAndReindexFromReset(
                mergeResult.items(), plan.getResetTimeSnapshot()
        );

        plan.replaceItems(orderedAdjusted);
        plan.updateAdjustmentSummary(result.getCoachMessage());
        plan.updateProgress();
        planRepository.save(plan);
        recordCompletionOnce(plan);
        return toResponse(plan, rewardCharacterIfCompleted(plan));
    }

    @Transactional
    // HH:mm과 IANA zoneId를 검증한 뒤 사용자별 설정을 저장한다.
    public RoutineSettingResponseDto updateSetting(Long userId, UpdateRoutineSettingRequestDto request) {
        LocalTime resetTime = LocalTime.parse(request.getResetTime(), TIME_FORMATTER);
        String zoneId = request.getZoneId() == null || request.getZoneId().isBlank()
                ? defaultZoneId
                : request.getZoneId();
        try {
            ZoneId.of(zoneId);
        } catch (DateTimeException exception) {
            throw new IllegalArgumentException("올바른 시간대(zoneId)를 입력해주세요.");
        }

        RoutineSetting setting = getOrCreateSetting(userId);
        setting.update(resetTime, zoneId);
        settingRepository.save(setting);
        return toSettingResponse(setting);
    }

    @Transactional
    public RoutineSettingResponseDto getSetting(Long userId) {
        return toSettingResponse(getOrCreateSetting(userId));
    }

    @Transactional(readOnly = true)
    public RoutineStatsResponseDto getStats(Long userId) {
        List<RoutineCompletionDto> completions = historyRepository
                .findAllByUserIdOrderByCompletedAtDesc(userId).stream()
                .map(history -> RoutineCompletionDto.builder()
                        .completedDate(history.getCompletedDate())
                        .completedAt(history.getCompletedAt())
                        .completionNumber(history.getCompletionNumber())
                        .build())
                .toList();
        return RoutineStatsResponseDto.builder()
                .totalCompletedCount(completions.size())
                .completions(completions)
                .build();
    }

    @Transactional
    public void rollOverUser(Long userId) {
        getOrRollOverTodayPlan(userId);
    }

    private DailyRoutinePlan getOrRollOverTodayPlan(Long userId) {
        // 초기화 시각이 지나 새 routineDate가 되면 이전 기본 스냅샷을 복제한다.
        RoutineSetting setting = getOrCreateSetting(userId);
        return findOrRollOverTodayPlan(userId, setting)
                .orElseThrow(() -> new IllegalStateException(
                        "생성된 루틴이 없습니다. 먼저 오늘의 AI 루틴을 생성해주세요."
                ));
    }

    private Optional<DailyRoutinePlan> findOrRollOverTodayPlan(
            Long userId,
            RoutineSetting setting
    ) {
        LocalDate today = calculateRoutineDate(setting);
        Optional<DailyRoutinePlan> todayPlan =
                planRepository.findByUserIdAndRoutineDate(userId, today);
        if (todayPlan.isPresent()) {
            return todayPlan;
        }

        return planRepository.findFirstByUserIdOrderByRoutineDateDesc(userId)
                .map(previous -> cloneForNewRoutineDate(
                        previous, today, setting.getResetTime()
                ));
    }

    private DailyRoutinePlan cloneForNewRoutineDate(
            DailyRoutinePlan previous,
            LocalDate routineDate,
            LocalTime resetTime
    ) {
        // 오늘만 변경된 항목이 아니라 최초 기본 루틴을 완료=false로 복원한다.
        List<DailyRoutineItem> baselineItems = readBaselineItems(previous);
        String baselineItemsJson = previous.getBaselineItemsJson();
        if (baselineItemsJson == null || baselineItemsJson.isBlank()
                || "[]".equals(baselineItemsJson.trim())) {
            baselineItemsJson = serializeBaselineItems(baselineItems);
        }

        DailyRoutinePlan plan = DailyRoutinePlan.builder()
                .userId(previous.getUserId())
                .routineDate(routineDate)
                .resetTimeSnapshot(resetTime)
                .baselineItemsJson(baselineItemsJson)
                .adjustmentSummary("일정 변경이 종료되어 기본 루틴으로 돌아왔습니다.")
                .build();

        baselineItems.stream()
                .sorted(Comparator.comparingInt(DailyRoutineItem::getSortOrder))
                .forEach(item -> plan.addItem(copyItem(item, false, item.getSortOrder())));
        plan.updateProgress();
        return planRepository.save(plan);
    }

    private List<DailyRoutineItem> toNewItems(
            List<AiRoutineItemDto> aiItems,
            Set<RoutineCategory> selectedCategories,
            Map<Long, Boolean> completedById
    ) {
        List<DailyRoutineItem> items = new ArrayList<>();
        int order = 0;
        for (AiRoutineItemDto aiItem : aiItems) {
            RoutineCategory category;
            LocalTime scheduledTime;
            try {
                category = RoutineCategory.valueOf(aiItem.getCategory());
                scheduledTime = parseAiScheduledTime(aiItem.getScheduledTime());
            } catch (IllegalArgumentException exception) {
                continue;
            }

            if (scheduledTime == null) {
                continue;
            }

            // 사용자가 선택하지 않은 카테고리나 내용이 비어 있는 AI 항목은 저장하지 않는다.
            if (!selectedCategories.contains(category)
                    || aiItem.getTitle() == null || aiItem.getTitle().isBlank()
                    || aiItem.getDescription() == null || aiItem.getDescription().isBlank()) {
                continue;
            }

            boolean completed = aiItem.getOriginalItemId() != null
                    && Boolean.TRUE.equals(completedById.get(aiItem.getOriginalItemId()));
            items.add(DailyRoutineItem.builder()
                    .category(category)
                    .title(aiItem.getTitle().trim())
                    .description(aiItem.getDescription().trim())
                    .scheduledTime(scheduledTime)
                    .completionWindowMinutes(normalizeWindowMinutes(
                            aiItem.getCompletionWindowMinutes()
                    ))
                    .sortOrder(order++)
                    .completed(completed)
                    .build());
        }

        if (items.isEmpty()) {
            throw new IllegalStateException("선택한 범위에 맞는 AI 루틴을 생성하지 못했습니다.");
        }
        ensureSelectedCategoriesPresent(items, selectedCategories);
        return items;
    }

    private AdjustmentMergeResult mergeAdjustedItems(
            List<AiRoutineItemDto> aiItems,
            Set<RoutineCategory> selectedCategories,
            List<DailyRoutineItem> currentItems
    ) {
        Map<Long, DailyRoutineItem> currentById = new HashMap<>();
        currentItems.stream()
                .filter(item -> item.getId() != null)
                .forEach(item -> currentById.put(item.getId(), item));

        List<DailyRoutineItem> merged = new ArrayList<>();
        Set<Long> adjustedOriginalIds = new HashSet<>();
        int appliedChanges = 0;
        int newlyAddedItems = 0;

        for (AiRoutineItemDto aiItem : aiItems == null ? List.<AiRoutineItemDto>of() : aiItems) {
            RoutineCategory category;
            try {
                category = RoutineCategory.valueOf(aiItem.getCategory());
            } catch (IllegalArgumentException | NullPointerException exception) {
                continue;
            }

            LocalTime scheduledTime = parseAiScheduledTime(aiItem.getScheduledTime());
            if (!selectedCategories.contains(category)
                    || scheduledTime == null
                    || aiItem.getTitle() == null || aiItem.getTitle().isBlank()
                    || aiItem.getDescription() == null || aiItem.getDescription().isBlank()) {
                continue;
            }

            DailyRoutineItem original = null;
            if (aiItem.getOriginalItemId() != null) {
                original = currentById.get(aiItem.getOriginalItemId());
                // 존재하지 않는 ID, 중복 변경, 카테고리 변경, 완료·마감 항목 변경은 모두 거부한다.
                if (original == null
                        || adjustedOriginalIds.contains(aiItem.getOriginalItemId())
                        || original.getCategory() != category
                        || original.isCompleted()
                        || original.isMissed()) {
                    continue;
                }
                if (hasSameRoutineContent(original, aiItem, scheduledTime)) {
                    continue;
                }
                adjustedOriginalIds.add(aiItem.getOriginalItemId());
            } else if (currentItems.size() + newlyAddedItems >= MAX_ROUTINE_ITEMS) {
                // 일시적인 일정 조정에서도 루틴 총개수는 10개를 넘지 않는다.
                continue;
            } else {
                newlyAddedItems++;
            }

            merged.add(DailyRoutineItem.builder()
                    .category(category)
                    .title(aiItem.getTitle().trim())
                    .description(aiItem.getDescription().trim())
                    .scheduledTime(scheduledTime)
                    .completionWindowMinutes(normalizeWindowMinutes(
                            aiItem.getCompletionWindowMinutes()
                    ))
                    .sortOrder(merged.size())
                    .completed(false)
                    .build());
            appliedChanges++;
        }

        // AI가 생략했거나 잘못 반환한 항목도 사라지지 않게 원본을 그대로 합친다.
        currentItems.stream()
                .filter(item -> selectedCategories.contains(item.getCategory()))
                .filter(item -> item.getId() == null || !adjustedOriginalIds.contains(item.getId()))
                .forEach(item -> merged.add(copyItem(item, item.isCompleted(), merged.size())));

        if (merged.isEmpty()) {
            throw new IllegalStateException("조정할 수 있는 루틴 항목이 없습니다.");
        }
        ensureSelectedCategoriesPresent(merged, selectedCategories);
        return new AdjustmentMergeResult(merged, appliedChanges);
    }

    RegenerationMergeResult mergeRegeneratedItems(
            DailyRoutinePlan previousPlan,
            DailyRoutinePlan targetPlan,
            List<DailyRoutineItem> previousItems,
            List<DailyRoutineItem> aiGeneratedItems,
            Set<RoutineCategory> selectedCategories,
            LocalDateTime now
    ) {
        List<DailyRoutineItem> preservedPast = previousItems.stream()
                .filter(item -> isAlreadyStarted(previousPlan, item, now))
                .sorted(Comparator.comparingInt(DailyRoutineItem::getSortOrder))
                .map(item -> copyItem(item, item.isCompleted(), item.getSortOrder()))
                .toList();
        List<DailyRoutineItem> previousFuture = previousItems.stream()
                .filter(item -> !isAlreadyStarted(previousPlan, item, now))
                .sorted(Comparator.comparingInt(DailyRoutineItem::getSortOrder))
                .toList();

        int futureSlots = Math.max(0, aiGeneratedItems.size() - preservedPast.size());
        List<DailyRoutineItem> futureCandidates = aiGeneratedItems.stream()
                .filter(item -> now.isBefore(calculateAvailableAt(targetPlan, item)))
                .sorted(Comparator.comparing(item -> calculateAvailableAt(targetPlan, item)))
                .toList();
        List<DailyRoutineItem> selectedFuture = selectDistributedFutureItems(
                futureCandidates,
                futureSlots,
                preservedPast,
                selectedCategories
        );

        // AI 후보가 부족할 때만 기존의 아직 시작하지 않은 항목으로 빈자리를 채운다.
        for (DailyRoutineItem fallback : previousFuture) {
            if (selectedFuture.size() >= futureSlots) {
                break;
            }
            selectedFuture.add(copyItem(fallback, fallback.isCompleted(), selectedFuture.size()));
        }

        List<DailyRoutineItem> merged = new ArrayList<>(preservedPast);
        merged.addAll(selectedFuture.stream()
                .limit(futureSlots)
                .map(item -> copyItem(item, false, merged.size()))
                .toList());
        if (merged.size() > MAX_ROUTINE_ITEMS) {
            throw new IllegalStateException("루틴은 최대 10개까지만 생성할 수 있습니다.");
        }
        return new RegenerationMergeResult(
                merged,
                preservedPast.size(),
                selectedFuture.stream().limit(futureSlots).toList().size()
        );
    }

    private int determinePersistentRoutineCount(
            int previousCount,
            int generatedCount,
            Set<RoutineCategory> selectedCategories
    ) {
        int minimumCount = selectedCategories.size();
        // 수면·기상을 선택한 경우 기상과 취침이 각각 필요하므로 한 자리를 추가한다.
        if (selectedCategories.contains(RoutineCategory.SLEEP_WAKE)) {
            minimumCount++;
        }
        int desiredCount = previousCount > 0
                ? Math.max(previousCount, minimumCount)
                : generatedCount;
        return Math.min(MAX_ROUTINE_ITEMS, Math.min(desiredCount, generatedCount));
    }

    private List<DailyRoutineItem> selectPersistentBaselineItems(
            List<DailyRoutineItem> candidates,
            int count,
            Set<RoutineCategory> selectedCategories
    ) {
        if (candidates.size() <= count) {
            return new ArrayList<>(candidates);
        }

        List<DailyRoutineItem> selected = new ArrayList<>();
        Set<DailyRoutineItem> used = new HashSet<>();

        if (selectedCategories.contains(RoutineCategory.SLEEP_WAKE)) {
            addBestCandidate(candidates, selected, used, item ->
                    item.getCategory() == RoutineCategory.SLEEP_WAKE
                            && (containsWakeKeyword(item.getTitle())
                            || containsWakeKeyword(item.getDescription())));
            addBestCandidate(candidates, selected, used, item ->
                    item.getCategory() == RoutineCategory.SLEEP_WAKE
                            && (containsSleepKeyword(item.getTitle())
                            || containsSleepKeyword(item.getDescription())));
        }

        for (RoutineCategory category : selectedCategories) {
            if (selected.size() >= count || category == RoutineCategory.SLEEP_WAKE) {
                continue;
            }
            addBestCandidate(candidates, selected, used,
                    item -> item.getCategory() == category);
        }

        while (selected.size() < count && used.size() < candidates.size()) {
            addBestCandidate(candidates, selected, used, item -> true);
        }
        ensureSelectedCategoriesPresent(selected, selectedCategories);
        return selected;
    }

    private void addBestCandidate(
            List<DailyRoutineItem> candidates,
            List<DailyRoutineItem> selected,
            Set<DailyRoutineItem> used,
            java.util.function.Predicate<DailyRoutineItem> condition
    ) {
        var matching = candidates.stream()
                .filter(item -> !used.contains(item))
                .filter(condition);
        DailyRoutineItem candidate = selected.isEmpty()
                ? matching.min(Comparator.comparing(DailyRoutineItem::getScheduledTime)).orElse(null)
                : matching.max(Comparator.comparingInt(
                        item -> minimumTimeDistance(item, selected)
                )).orElse(null);
        if (candidate != null) {
            selected.add(candidate);
            used.add(candidate);
        }
    }

    private List<DailyRoutineItem> selectDistributedFutureItems(
            List<DailyRoutineItem> candidates,
            int count,
            List<DailyRoutineItem> preserved,
            Set<RoutineCategory> selectedCategories
    ) {
        if (count <= 0 || candidates.isEmpty()) {
            return new ArrayList<>();
        }

        List<DailyRoutineItem> selected = new ArrayList<>();
        Set<DailyRoutineItem> used = new HashSet<>();
        Set<RoutineCategory> covered = new HashSet<>();
        preserved.forEach(item -> covered.add(item.getCategory()));

        // 이미 지난 항목에 없는 선택 카테고리를 우선 확보한다.
        for (RoutineCategory category : selectedCategories) {
            if (selected.size() >= count || covered.contains(category)) {
                continue;
            }
            List<DailyRoutineItem> anchors = new ArrayList<>(preserved);
            anchors.addAll(selected);
            candidates.stream()
                    .filter(item -> item.getCategory() == category)
                    .filter(item -> !used.contains(item))
                    .max(Comparator.comparingInt(item -> minimumTimeDistance(item, anchors)))
                    .ifPresent(item -> {
                        selected.add(item);
                        used.add(item);
                        covered.add(category);
                    });
        }

        // 남은 자리는 기존 선택 시각들과 가장 멀리 떨어진 후보부터 골라 하루에 고르게 분산한다.
        while (selected.size() < count && used.size() < candidates.size()) {
            List<DailyRoutineItem> anchors = new ArrayList<>(preserved);
            anchors.addAll(selected);
            DailyRoutineItem next = candidates.stream()
                    .filter(item -> !used.contains(item))
                    .max(Comparator.comparingInt(item -> minimumTimeDistance(item, anchors)))
                    .orElse(null);
            if (next == null) {
                break;
            }
            selected.add(next);
            used.add(next);
        }
        return selected;
    }

    private int minimumTimeDistance(
            DailyRoutineItem candidate,
            List<DailyRoutineItem> anchors
    ) {
        if (anchors.isEmpty()) {
            return candidate.getScheduledTime().toSecondOfDay();
        }
        return anchors.stream()
                .mapToInt(anchor -> circularTimeDistance(
                        candidate.getScheduledTime(), anchor.getScheduledTime()
                ))
                .min()
                .orElse(0);
    }

    private int circularTimeDistance(LocalTime first, LocalTime second) {
        int difference = Math.abs(first.toSecondOfDay() - second.toSecondOfDay());
        return Math.min(difference, 24 * 60 * 60 - difference);
    }

    private boolean isAlreadyStarted(
            DailyRoutinePlan plan,
            DailyRoutineItem item,
            LocalDateTime now
    ) {
        return !now.isBefore(calculateAvailableAt(plan, item));
    }

    record RegenerationMergeResult(
            List<DailyRoutineItem> items,
            int preservedPastCount,
            int replacedFutureCount
    ) {
    }

    private boolean hasSameRoutineContent(
            DailyRoutineItem original,
            AiRoutineItemDto adjusted,
            LocalTime adjustedTime
    ) {
        return original.getTitle().equals(adjusted.getTitle().trim())
                && original.getDescription().equals(adjusted.getDescription().trim())
                && original.getScheduledTime().equals(adjustedTime)
                && normalizeWindowMinutes(original.getCompletionWindowMinutes())
                == normalizeWindowMinutes(adjusted.getCompletionWindowMinutes());
    }

    private record AdjustmentMergeResult(
            List<DailyRoutineItem> items,
            int appliedChanges
    ) {
    }

    private LocalTime parseAiScheduledTime(String rawTime) {
        if (rawTime == null || rawTime.isBlank()) {
            return null;
        }

        String value = rawTime.trim();
        try {
            return LocalTime.parse(value, TIME_FORMATTER);
        } catch (DateTimeParseException ignored) {
            // 이전 AI 응답과의 호환을 위해 초가 포함된 ISO 시각도 허용한다.
        }
        try {
            LocalTime parsed = LocalTime.parse(value, DateTimeFormatter.ISO_LOCAL_TIME);
            return parsed.withSecond(0).withNano(0);
        } catch (DateTimeParseException ignored) {
            // ISO 날짜·시간 문자열 안에 포함된 HH:mm도 아래에서 찾아낸다.
        }

        Matcher embeddedTime = EMBEDDED_TIME_PATTERN.matcher(value);
        if (embeddedTime.find()) {
            return LocalTime.of(
                    Integer.parseInt(embeddedTime.group(1)),
                    Integer.parseInt(embeddedTime.group(2))
            );
        }

        Matcher koreanTime = KOREAN_TIME_PATTERN.matcher(value);
        if (!koreanTime.find()) {
            return null;
        }
        int hour = Integer.parseInt(koreanTime.group(2));
        int minute = koreanTime.group(3) == null ? 0 : Integer.parseInt(koreanTime.group(3));
        if (minute > 59) {
            return null;
        }

        String meridiem = koreanTime.group(1);
        if (meridiem == null) {
            return hour <= 23 ? LocalTime.of(hour, minute) : null;
        }
        if (hour < 1 || hour > 12) {
            return null;
        }
        if ("오전".equals(meridiem)) {
            hour = hour == 12 ? 0 : hour;
        } else {
            hour = hour == 12 ? 12 : hour + 12;
        }
        return LocalTime.of(hour, minute);
    }

    private void ensureSelectedCategoriesPresent(
            List<DailyRoutineItem> items,
            Set<RoutineCategory> selectedCategories
    ) {
        Set<RoutineCategory> actualCategories = new HashSet<>();
        items.forEach(item -> actualCategories.add(item.getCategory()));
        Set<RoutineCategory> missingCategories = new HashSet<>(selectedCategories);
        missingCategories.removeAll(actualCategories);
        if (!missingCategories.isEmpty()) {
            throw new IllegalStateException("AI 루틴에서 선택한 범주가 누락되었습니다: " + missingCategories);
        }
    }

    private List<DailyRoutineItem> sortAndReindexFromReset(
            List<DailyRoutineItem> items,
            LocalTime resetTime
    ) {
        // 수면 루틴을 선택했으면 기상 시각부터, 아니면 AI가 판단한 초기화 시각부터 정렬한다.
        LocalTime routineStartTime = findRoutineStartTime(items, resetTime);
        List<DailyRoutineItem> sorted = items.stream()
                .sorted(Comparator.comparingInt(
                        item -> secondsAfterReset(item.getScheduledTime(), routineStartTime)
                ))
                .toList();
        return IntStream.range(0, sorted.size())
                .mapToObj(index -> {
                    DailyRoutineItem item = sorted.get(index);
                    return copyItem(item, item.isCompleted(), index);
                })
                .toList();
    }

    private int secondsAfterReset(LocalTime scheduledTime, LocalTime resetTime) {
        int difference = scheduledTime.toSecondOfDay() - resetTime.toSecondOfDay();
        return difference < 0 ? difference + 24 * 60 * 60 : difference;
    }

    private LocalTime findRoutineStartTime(
            List<DailyRoutineItem> items,
            LocalTime fallbackResetTime
    ) {
        return items.stream()
                .filter(item -> item.getCategory() == RoutineCategory.SLEEP_WAKE)
                .filter(item -> containsWakeKeyword(item.getTitle())
                        || containsWakeKeyword(item.getDescription()))
                .map(DailyRoutineItem::getScheduledTime)
                .findFirst()
                .orElse(fallbackResetTime);
    }

    private boolean containsWakeKeyword(String value) {
        return value != null && (value.contains("기상") || value.contains("일어나"));
    }

    private boolean containsSleepKeyword(String value) {
        return value != null && (value.contains("취침") || value.contains("잠자리")
                || value.contains("잠들") || value.contains("수면"));
    }

    private DailyRoutineItem copyItem(DailyRoutineItem source, boolean completed, int sortOrder) {
        DailyRoutineItem copy = DailyRoutineItem.builder()
                .category(source.getCategory())
                .title(source.getTitle())
                .description(source.getDescription())
                .scheduledTime(source.getScheduledTime())
                .completionWindowMinutes(normalizeWindowMinutes(
                        source.getCompletionWindowMinutes()
                ))
                .sortOrder(sortOrder)
                .completed(false)
                .build();
        if (completed) {
            copy.inheritCompletedStatus(source.getCompletedAt());
        }
        if (source.isMissed()) {
            copy.markMissed();
        }
        return copy;
    }

    private String serializeBaselineItems(List<DailyRoutineItem> items) {
        List<Map<String, Object>> snapshot = items.stream()
                .sorted(Comparator.comparingInt(DailyRoutineItem::getSortOrder))
                .map(item -> {
                    Map<String, Object> value = new HashMap<>();
                    value.put("category", item.getCategory().name());
                    value.put("title", item.getTitle());
                    value.put("description", item.getDescription());
                    value.put("scheduledTime", item.getScheduledTime().format(TIME_FORMATTER));
                    value.put("completionWindowMinutes", normalizeWindowMinutes(
                            item.getCompletionWindowMinutes()
                    ));
                    value.put("sortOrder", item.getSortOrder());
                    return value;
                })
                .toList();
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JacksonException exception) {
            throw new IllegalStateException("기본 루틴을 저장할 수 없습니다.", exception);
        }
    }

    private List<DailyRoutineItem> readBaselineItems(DailyRoutinePlan previous) {
        String snapshot = previous.getBaselineItemsJson();
        if (snapshot == null || snapshot.isBlank() || "[]".equals(snapshot.trim())) {
            // 기능 추가 전 생성된 데이터도 다음 초기화부터 사용할 수 있게 호환한다.
            return previous.getItems().stream()
                    .map(item -> copyItem(item, false, item.getSortOrder()))
                    .toList();
        }

        try {
            JsonNode root = objectMapper.readTree(snapshot);
            List<DailyRoutineItem> baselineItems = new ArrayList<>();
            root.forEach(item -> baselineItems.add(DailyRoutineItem.builder()
                    .category(RoutineCategory.valueOf(item.path("category").asText()))
                    .title(item.path("title").asText())
                    .description(item.path("description").asText())
                    .scheduledTime(LocalTime.parse(
                            item.path("scheduledTime").asText(), TIME_FORMATTER
                    ))
                    .completionWindowMinutes(normalizeWindowMinutes(
                            item.path("completionWindowMinutes").asInt(30)
                    ))
                    .sortOrder(item.path("sortOrder").asInt())
                    .completed(false)
                    .build()));
            if (baselineItems.isEmpty()) {
                throw new IllegalStateException("저장된 기본 루틴이 비어 있습니다.");
            }
            return baselineItems;
        } catch (JacksonException | IllegalArgumentException | DateTimeParseException exception) {
            throw new IllegalStateException("저장된 기본 루틴을 불러올 수 없습니다.", exception);
        }
    }

    private void recordCompletionOnce(DailyRoutinePlan plan) {
        // 한 계획은 처음 100%가 된 순간만 한 번 기록한다.
        if (plan.getCompletionPercentage() != 100 || historyRepository.existsByPlanId(plan.getId())) {
            return;
        }

        long completionNumber = historyRepository.countByUserId(plan.getUserId()) + 1;
        historyRepository.save(RoutineCompletionHistory.builder()
                .planId(plan.getId())
                .userId(plan.getUserId())
                .completedDate(plan.getRoutineDate())
                .completedAt(plan.getFirstCompletedAt())
                .completionNumber(completionNumber)
                .build());
    }

    private CharacterRewardResponseDto rewardCharacterIfCompleted(DailyRoutinePlan plan) {
        // character-service가 날짜별 중복 요청을 막으므로 재시도되어도 캐릭터는 한 번만 지급된다.
        if (plan.getCompletionPercentage() != 100) {
            return null;
        }
        return characterRewardClient.reward(plan.getUserId(), plan.getRoutineDate());
    }

    private int normalizeWindowMinutes(int minutes) {
        // AI가 비정상 값을 주더라도 제한 시간은 10~180분 범위로 보정한다.
        return Math.max(10, Math.min(minutes <= 0 ? 30 : minutes, 180));
    }

    private LocalDateTime calculateAvailableAt(
            DailyRoutinePlan plan,
            DailyRoutineItem item
    ) {
        // 초기화 시각보다 이른 시각(예: 00:30)은 달력상 다음 날에 실행되는 항목이다.
        LocalDate scheduledDate = item.getScheduledTime().isBefore(plan.getResetTimeSnapshot())
                ? plan.getRoutineDate().plusDays(1)
                : plan.getRoutineDate();
        return LocalDateTime.of(scheduledDate, item.getScheduledTime());
    }

    private LocalDateTime calculateDeadlineAt(
            DailyRoutinePlan plan,
            DailyRoutineItem item
    ) {
        return calculateAvailableAt(plan, item)
                .plusMinutes(normalizeWindowMinutes(item.getCompletionWindowMinutes()));
    }

    private LocalDateTime currentLocalDateTime(Long userId) {
        RoutineSetting setting = getOrCreateSetting(userId);
        return ZonedDateTime.now(ZoneId.of(setting.getZoneId())).toLocalDateTime();
    }

    private void validateItemEditable(DailyRoutinePlan plan, DailyRoutineItem item) {
        // 시작 전 완료와 마감 후 수정 모두 막아 보상 판정의 신뢰성을 유지한다.
        if (item.isMissed()) {
            throw new IllegalArgumentException(
                    "완료 제한 시간이 지나 미완료로 확정된 루틴은 수정할 수 없습니다."
            );
        }
        LocalDateTime now = currentLocalDateTime(plan.getUserId());
        LocalDateTime availableAt = calculateAvailableAt(plan, item);
        LocalDateTime deadlineAt = calculateDeadlineAt(plan, item);
        if (now.isBefore(availableAt)) {
            throw new IllegalArgumentException(
                    "이 루틴은 " + availableAt + "부터 완료할 수 있습니다."
            );
        }
        if (now.isAfter(deadlineAt)) {
            throw new IllegalArgumentException(
                    "완료 제한 시간이 지나 미완료로 확정된 루틴은 수정할 수 없습니다."
            );
        }
    }

    private void refreshExpiredLocks(DailyRoutinePlan plan) {
        // 마감이 지나면 missed=true로 저장하여 이후 false 상태를 변경하지 못하게 한다.
        LocalDateTime now = currentLocalDateTime(plan.getUserId());
        boolean changed = false;
        for (DailyRoutineItem item : plan.getItems()) {
            if (!item.isCompleted() && !item.isMissed()
                    && now.isAfter(calculateDeadlineAt(plan, item))) {
                item.markMissed();
                changed = true;
            }
        }
        if (changed) {
            itemRepository.saveAll(plan.getItems());
            plan.updateProgress();
            planRepository.save(plan);
        }
    }

    private RoutineSetting getOrCreateSetting(Long userId) {
        return settingRepository.findByUserId(userId)
                .orElseGet(() -> settingRepository.save(RoutineSetting.builder()
                        .userId(userId)
                        .resetTime(defaultResetTime)
                        .zoneId(defaultZoneId)
                        .build()));
    }

    private LocalDate calculateRoutineDate(RoutineSetting setting) {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of(setting.getZoneId()));
        return now.toLocalTime().isBefore(setting.getResetTime())
                ? now.toLocalDate().minusDays(1)
                : now.toLocalDate();
    }

    private void validateSelectedCategories(Set<RoutineCategory> selectedCategories) {
        if (selectedCategories == null || selectedCategories.isEmpty()) {
            throw new IllegalArgumentException(
                    "user-service에서 받고 싶은 루틴을 하나 이상 선택해주세요."
            );
        }
    }

    private LocalTime parseAiResetTime(String resetTime) {
        try {
            return LocalTime.parse(resetTime, TIME_FORMATTER);
        } catch (DateTimeParseException exception) {
            throw new IllegalStateException("AI가 추천한 초기화 시각 형식이 올바르지 않습니다.", exception);
        }
    }

    private DailyRoutineResponseDto toResponse(DailyRoutinePlan plan) {
        return toResponse(plan, null);
    }

    private DailyRoutineResponseDto toResponse(
            DailyRoutinePlan plan,
            CharacterRewardResponseDto characterReward
    ) {
        refreshExpiredLocks(plan);
        LocalDateTime now = currentLocalDateTime(plan.getUserId());
        LocalTime routineStartTime = findRoutineStartTime(
                plan.getItems(), plan.getResetTimeSnapshot()
        );
        List<DailyRoutineItem> orderedItems = plan.getItems().stream()
                .sorted(Comparator.comparingInt(
                        item -> secondsAfterReset(item.getScheduledTime(), routineStartTime)
                ))
                .toList();
        List<RoutineItemResponseDto> items = IntStream.range(0, orderedItems.size())
                .mapToObj(index -> {
                    DailyRoutineItem item = orderedItems.get(index);
                    LocalDateTime availableAt = calculateAvailableAt(plan, item);
                    LocalDateTime deadlineAt = calculateDeadlineAt(plan, item);
                    boolean expired = item.isMissed();
                    boolean editable = !item.isMissed()
                            && !now.isBefore(availableAt) && !now.isAfter(deadlineAt);
                    return RoutineItemResponseDto.builder()
                        .id(item.getId())
                        .category(item.getCategory().name())
                        .title(item.getTitle())
                        .description(item.getDescription())
                        .scheduledTime(item.getScheduledTime())
                        .completionWindowMinutes(normalizeWindowMinutes(
                                item.getCompletionWindowMinutes()
                        ))
                        .availableAt(availableAt)
                        .deadlineAt(deadlineAt)
                        .sortOrder(index)
                        .completed(item.isCompleted())
                        .completedAt(item.getCompletedAt())
                        .expired(expired)
                        .editable(editable)
                        .build();
                })
                .toList();
        return DailyRoutineResponseDto.builder()
                .routineGenerated(true)
                .planId(plan.getId())
                .routineDate(plan.getRoutineDate())
                .resetTime(plan.getResetTimeSnapshot())
                .completionPercentage(plan.getCompletionPercentage())
                .allCompleted(plan.getCompletionPercentage() == 100)
                .firstCompletedAt(plan.getFirstCompletedAt())
                .characterReward(characterReward)
                .coachMessage(plan.getAdjustmentSummary())
                .items(items)
                .build();
    }

    private DailyRoutineResponseDto toEmptyResponse(RoutineSetting setting) {
        return DailyRoutineResponseDto.builder()
                .routineGenerated(false)
                .routineDate(calculateRoutineDate(setting))
                .resetTime(setting.getResetTime())
                .completionPercentage(0)
                .allCompleted(false)
                .coachMessage("아직 생성된 루틴이 없습니다. 생성 버튼을 눌러 오늘의 AI 루틴을 만들어주세요.")
                .items(List.of())
                .build();
    }

    private RoutineSettingResponseDto toSettingResponse(RoutineSetting setting) {
        return RoutineSettingResponseDto.builder()
                .resetTime(setting.getResetTime())
                .zoneId(setting.getZoneId())
                .build();
    }
}
