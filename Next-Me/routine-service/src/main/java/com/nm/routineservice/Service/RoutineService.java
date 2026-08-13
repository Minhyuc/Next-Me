package com.nm.routineservice.Service;

import com.nm.routineservice.Dto.AdjustRoutineRequestDto;
import com.nm.routineservice.Dto.AiRoutineItemDto;
import com.nm.routineservice.Dto.AiRoutineResultDto;
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
import java.util.Set;
import java.util.stream.IntStream;

@Service
/** 루틴의 생성부터 일일 초기화, 시간 제한, 완료율, 완료 이력까지 관리하는 핵심 서비스다. */
public class RoutineService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final RoutineSettingRepository settingRepository;
    private final DailyRoutinePlanRepository planRepository;
    private final DailyRoutineItemRepository itemRepository;
    private final RoutineCompletionHistoryRepository historyRepository;
    private final UserRoutineContextClient contextClient;
    private final OpenAiRoutineService openAiRoutineService;
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
        this.objectMapper = objectMapper;
        this.defaultResetTime = LocalTime.parse(defaultResetTime, TIME_FORMATTER);
        this.defaultZoneId = defaultZoneId;
    }

    @Transactional
    // 현재 선호도와 같은 계획은 재사용하고, 선호도가 바뀌면 새 기본 루틴을 생성한다.
    public DailyRoutineResponseDto generateToday(
            Long userId,
            String authorizationHeader,
            GenerateRoutineRequestDto request
    ) {
        // user-service에서 현재 프로필과 선택한 루틴 카테고리를 가져온다.
        UserRoutineContextDto context = contextClient.getContext(authorizationHeader);
        validateSelectedCategories(context.getSelectedCategories());

        RoutineSetting setting = getOrCreateSetting(userId);
        LocalDate existingRoutineDate = calculateRoutineDate(setting);
        var existingPlan = planRepository.findByUserIdAndRoutineDate(userId, existingRoutineDate);
        if (existingPlan.isPresent()
                && hasSameCategories(existingPlan.get(), context.getSelectedCategories())) {
            return toResponse(existingPlan.get());
        }

        ZonedDateTime currentDateTime = ZonedDateTime.now(ZoneId.of(setting.getZoneId()));
        AiRoutineResultDto aiResult = openAiRoutineService.generate(
                context,
                request == null ? null : request.getAdditionalRequest(),
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

        List<DailyRoutineItem> baselineItems = toNewItems(
                aiResult.getItems(), context.getSelectedCategories(), Map.of()
        );
        baselineItems = sortAndReindexFromReset(baselineItems, aiResetTime);

        // 일정 조정 전 원본을 스냅샷으로 저장해 다음 루틴 날짜에 복원한다.
        plan.updateBaselineItemsJson(serializeBaselineItems(baselineItems));

        // 완료 또는 마감 상태가 생긴 현재 계획은 다시 열지 않고 다음 루틴부터 새 선호도를 적용한다.
        if (hasLockedItems(plan)) {
            plan.updateAdjustmentSummary(
                    "변경한 루틴 종류를 저장했어요. 현재 완료·마감 기록은 유지하고 다음 초기화부터 적용합니다."
            );
            planRepository.save(plan);
            return toResponse(plan);
        }

        plan.updateResetTimeSnapshot(aiResetTime);
        plan.replaceItems(baselineItems);
        plan.updateAdjustmentSummary(aiResult.getCoachMessage());
        plan.updateProgress();
        planRepository.save(plan);
        return toResponse(plan);
    }

    @Transactional
    // 조회할 때도 마감 시간이 지난 미완료 항목을 영구 실패 상태로 갱신한다.
    public DailyRoutineResponseDto getToday(Long userId) {
        return toResponse(getOrRollOverTodayPlan(userId));
    }

    @Transactional
    // 완료 변경 후 계획의 퍼센트와 최초 100% 완료 이력을 함께 갱신한다.
    public RoutineProgressResponseDto getTodayProgress(Long userId) {
        DailyRoutinePlan plan = getOrRollOverTodayPlan(userId);
        refreshExpiredLocks(plan);
        int totalCount = plan.getItems().size();
        int completedCount = (int) plan.getItems().stream()
                .filter(DailyRoutineItem::isCompleted)
                .count();

        return RoutineProgressResponseDto.builder()
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
        return toResponse(today);
    }

    @Transactional
    public DailyRoutineResponseDto adjustToday(
            Long userId,
            String authorizationHeader,
            AdjustRoutineRequestDto request
    ) {
        DailyRoutinePlan plan = getOrRollOverTodayPlan(userId);
        UserRoutineContextDto context = contextClient.getContext(authorizationHeader);
        validateSelectedCategories(context.getSelectedCategories());

        List<DailyRoutineItem> before = new ArrayList<>(plan.getItems());
        refreshExpiredLocks(plan);
        before = new ArrayList<>(plan.getItems());
        AiRoutineResultDto result = openAiRoutineService.adjust(
                context, before, plan.getRoutineDate(), request.getMessage()
        );

        Map<Long, Boolean> completedById = new HashMap<>();
        before.forEach(item -> completedById.put(item.getId(), item.isCompleted()));

        // 이미 마감 실패한 항목은 AI가 다시 살리거나 수정하지 못하게 제외한다.
        Set<Long> missedIds = new HashSet<>();
        // AI 응답에서 빠져도 이미 완료된 항목은 보상 계산을 위해 유지한다.
        before.stream()
                .filter(DailyRoutineItem::isMissed)
                .map(DailyRoutineItem::getId)
                .forEach(missedIds::add);

        List<DailyRoutineItem> adjusted = toNewItems(
                result.getItems().stream()
                        .filter(item -> item.getOriginalItemId() == null
                                || !missedIds.contains(item.getOriginalItemId()))
                        .toList(),
                context.getSelectedCategories(), completedById
        );

        Set<Long> referencedIds = new HashSet<>();
        result.getItems().stream()
                .map(AiRoutineItemDto::getOriginalItemId)
                .filter(id -> id != null)
                .forEach(referencedIds::add);

        // 마감 실패 항목도 영구 false 기록으로 유지한다.
        before.stream()
                .filter(DailyRoutineItem::isCompleted)
                .filter(item -> context.getSelectedCategories().contains(item.getCategory()))
                .filter(item -> !referencedIds.contains(item.getId()))
                .forEach(item -> adjusted.add(copyItem(item, true, adjusted.size())));

        before.stream()
                .filter(DailyRoutineItem::isMissed)
                .filter(item -> context.getSelectedCategories().contains(item.getCategory()))
                .forEach(item -> adjusted.add(copyItem(item, false, adjusted.size())));

        List<DailyRoutineItem> orderedAdjusted = sortAndReindexFromReset(
                adjusted, plan.getResetTimeSnapshot()
        );

        plan.replaceItems(orderedAdjusted);
        plan.updateAdjustmentSummary(result.getCoachMessage());
        plan.updateProgress();
        planRepository.save(plan);
        recordCompletionOnce(plan);
        return toResponse(plan);
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
        LocalDate today = calculateRoutineDate(setting);

        return planRepository.findByUserIdAndRoutineDate(userId, today)
                .orElseGet(() -> planRepository.findFirstByUserIdOrderByRoutineDateDesc(userId)
                        .map(previous -> cloneForNewRoutineDate(previous, today, setting.getResetTime()))
                        .orElseThrow(() -> new IllegalStateException(
                                "생성된 루틴이 없습니다. 먼저 오늘의 AI 루틴을 생성해주세요."
                        )));
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
                scheduledTime = LocalTime.parse(aiItem.getScheduledTime(), TIME_FORMATTER);
            } catch (IllegalArgumentException | DateTimeParseException exception) {
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

    private boolean hasSameCategories(
            DailyRoutinePlan plan,
            Set<RoutineCategory> selectedCategories
    ) {
        Set<RoutineCategory> currentCategories = new HashSet<>();
        // 일정 조정된 현재 항목보다 다음 날 복원될 기본 스냅샷을 기준으로 비교한다.
        readBaselineItems(plan).forEach(item -> currentCategories.add(item.getCategory()));
        return currentCategories.equals(selectedCategories);
    }

    private boolean hasLockedItems(DailyRoutinePlan plan) {
        return plan.getItems().stream()
                .anyMatch(item -> item.isCompleted() || item.isMissed());
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
            copy.changeCompleted(true);
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
                .planId(plan.getId())
                .routineDate(plan.getRoutineDate())
                .resetTime(plan.getResetTimeSnapshot())
                .completionPercentage(plan.getCompletionPercentage())
                .allCompleted(plan.getCompletionPercentage() == 100)
                .firstCompletedAt(plan.getFirstCompletedAt())
                .coachMessage(plan.getAdjustmentSummary())
                .items(items)
                .build();
    }

    private RoutineSettingResponseDto toSettingResponse(RoutineSetting setting) {
        return RoutineSettingResponseDto.builder()
                .resetTime(setting.getResetTime())
                .zoneId(setting.getZoneId())
                .build();
    }
}
