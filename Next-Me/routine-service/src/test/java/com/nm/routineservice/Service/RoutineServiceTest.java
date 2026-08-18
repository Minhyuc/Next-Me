package com.nm.routineservice.Service;

import com.nm.routineservice.Dto.DailyRoutineResponseDto;
import com.nm.routineservice.Dto.AdjustRoutineRequestDto;
import com.nm.routineservice.Dto.AiRoutineItemDto;
import com.nm.routineservice.Dto.AiRoutineResultDto;
import com.nm.routineservice.Dto.GenerateRoutineRequestDto;
import com.nm.routineservice.Dto.RoutineProgressResponseDto;
import com.nm.routineservice.Dto.UserRoutineContextDto;
import com.nm.routineservice.Entity.DailyRoutineItem;
import com.nm.routineservice.Entity.DailyRoutinePlan;
import com.nm.routineservice.Entity.RoutineCategory;
import com.nm.routineservice.Entity.RoutineSetting;
import com.nm.routineservice.Repository.DailyRoutineItemRepository;
import com.nm.routineservice.Repository.DailyRoutinePlanRepository;
import com.nm.routineservice.Repository.RoutineCompletionHistoryRepository;
import com.nm.routineservice.Repository.RoutineSettingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RoutineServiceTest {

    private RoutineSettingRepository settingRepository;
    private DailyRoutinePlanRepository planRepository;
    private DailyRoutineItemRepository itemRepository;
    private RoutineCompletionHistoryRepository historyRepository;
    private UserRoutineContextClient contextClient;
    private OpenAiRoutineService openAiRoutineService;
    private CharacterRewardClient characterRewardClient;
    private RoutineService routineService;

    @BeforeEach
    void setUp() {
        settingRepository = mock(RoutineSettingRepository.class);
        planRepository = mock(DailyRoutinePlanRepository.class);
        itemRepository = mock(DailyRoutineItemRepository.class);
        historyRepository = mock(RoutineCompletionHistoryRepository.class);
        contextClient = mock(UserRoutineContextClient.class);
        openAiRoutineService = mock(OpenAiRoutineService.class);
        characterRewardClient = mock(CharacterRewardClient.class);
        RoutineSetting setting = RoutineSetting.builder()
                .userId(1L)
                .resetTime(LocalTime.of(6, 0))
                .zoneId("Asia/Seoul")
                .build();

        when(settingRepository.findByUserId(1L)).thenReturn(Optional.of(setting));
        when(planRepository.findByUserIdAndRoutineDate(eq(1L), any()))
                .thenReturn(Optional.empty());
        when(planRepository.findFirstByUserIdOrderByRoutineDateDesc(1L))
                .thenReturn(Optional.empty());

        routineService = new RoutineService(
                settingRepository,
                planRepository,
                itemRepository,
                historyRepository,
                contextClient,
                openAiRoutineService,
                characterRewardClient,
                new ObjectMapper(),
                "06:00",
                "Asia/Seoul"
        );
    }

    @Test
    void todayReturnsEmptyStateBeforeFirstRoutineGeneration() {
        DailyRoutineResponseDto response = routineService.getToday(1L);

        assertFalse(response.isRoutineGenerated());
        assertNull(response.getPlanId());
        assertNotNull(response.getRoutineDate());
        assertEquals(LocalTime.of(6, 0), response.getResetTime());
        assertEquals(0, response.getCompletionPercentage());
        assertFalse(response.isAllCompleted());
        assertTrue(response.getItems().isEmpty());
    }

    @Test
    void progressReturnsEmptyStateBeforeFirstRoutineGeneration() {
        RoutineProgressResponseDto response = routineService.getTodayProgress(1L);

        assertFalse(response.isRoutineGenerated());
        assertNull(response.getPlanId());
        assertEquals(0, response.getCompletedCount());
        assertEquals(0, response.getTotalCount());
        assertEquals(0, response.getCompletionPercentage());
        assertFalse(response.isAllCompleted());
    }

    @Test
    void todayReturnsGeneratedStateWhenRoutinePlanExists() {
        DailyRoutinePlan plan = DailyRoutinePlan.builder()
                .id(10L)
                .userId(1L)
                .routineDate(LocalDate.of(2026, 8, 14))
                .resetTimeSnapshot(LocalTime.of(6, 0))
                .build();
        when(planRepository.findByUserIdAndRoutineDate(eq(1L), any()))
                .thenReturn(Optional.of(plan));

        DailyRoutineResponseDto response = routineService.getToday(1L);

        assertTrue(response.isRoutineGenerated());
        assertEquals(10L, response.getPlanId());
    }

    @Test
    void nextRoutineDateRestoresPersistentBaselineAfterTemporaryAdjustment() {
        DailyRoutinePlan previous = DailyRoutinePlan.builder()
                .id(30L)
                .userId(1L)
                .routineDate(LocalDate.now().minusDays(1))
                .resetTimeSnapshot(LocalTime.of(6, 0))
                .baselineItemsJson("""
                        [{"category":"DIET","title":"평소 저녁 식사","description":"평소 시간에 식사하세요.","scheduledTime":"18:30","completionWindowMinutes":60,"sortOrder":0}]
                        """)
                .build();
        previous.addItem(DailyRoutineItem.builder()
                .category(RoutineCategory.DIET)
                .title("야근 날 저녁 식사")
                .description("오늘만 늦게 식사하세요.")
                .scheduledTime(LocalTime.of(21, 30))
                .completionWindowMinutes(60)
                .sortOrder(0)
                .build());
        when(planRepository.findByUserIdAndRoutineDate(eq(1L), any()))
                .thenReturn(Optional.empty());
        when(planRepository.findFirstByUserIdOrderByRoutineDateDesc(1L))
                .thenReturn(Optional.of(previous));
        when(planRepository.save(any(DailyRoutinePlan.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DailyRoutineResponseDto response = routineService.getToday(1L);

        assertEquals(1, response.getItems().size());
        assertEquals("평소 저녁 식사", response.getItems().get(0).getTitle());
        assertEquals(LocalTime.of(18, 30), response.getItems().get(0).getScheduledTime());
        assertEquals("일정 변경이 종료되어 기본 루틴으로 돌아왔습니다.", response.getCoachMessage());
    }

    @Test
    void naturalLanguageAdjustmentChangesOnlyAffectedRoutineAndPreservesOthers() {
        DailyRoutinePlan plan = planWithTwoItems();
        String persistentBaseline = "[{\"title\":\"평소 기본 루틴\"}]";
        plan.updateBaselineItemsJson(persistentBaseline);
        when(planRepository.findByUserIdAndRoutineDate(eq(1L), any()))
                .thenReturn(Optional.of(plan));

        UserRoutineContextDto context = new UserRoutineContextDto(
                "{}", "{}", EnumSet.of(RoutineCategory.DIET, RoutineCategory.EXERCISE)
        );
        when(contextClient.getContext("Bearer test-token")).thenReturn(context);
        when(openAiRoutineService.adjust(eq(context), anyList(), eq(plan.getRoutineDate()), anyString()))
                .thenReturn(new AiRoutineResultDto(
                        "19시 퇴근 일정의 영향을 받는 저녁 식사만 조정했어요.",
                        "06:00",
                        List.of(new AiRoutineItemDto(
                                11L,
                                "DIET",
                                "저녁 식사하기",
                                "퇴근 후 소화에 부담이 적은 저녁 식사를 하세요.",
                                "21:30:00",
                                40
                        ))
                ));

        String message = "오늘은 오후 7시에 퇴근할 거야. 영향을 받는 루틴만 조정해줘";
        DailyRoutineResponseDto response = routineService.adjustToday(
                1L,
                "Bearer test-token",
                adjustRequest(message)
        );

        assertEquals(2, response.getItems().size());
        assertEquals(LocalTime.of(21, 30), response.getItems().stream()
                .filter(item -> "DIET".equals(item.getCategory()))
                .findFirst()
                .orElseThrow()
                .getScheduledTime());
        assertEquals(LocalTime.of(20, 0), response.getItems().stream()
                .filter(item -> "EXERCISE".equals(item.getCategory()))
                .findFirst()
                .orElseThrow()
                .getScheduledTime());
        assertEquals("19시 퇴근 일정의 영향을 받는 저녁 식사만 조정했어요.", response.getCoachMessage());
        assertEquals(persistentBaseline, plan.getBaselineItemsJson());
        verify(openAiRoutineService).adjust(eq(context), anyList(), eq(plan.getRoutineDate()), eq(message));
        verify(planRepository, atLeastOnce()).save(plan);
    }

    @Test
    void unusableAiTimeDoesNotDeleteExistingRoutine() {
        DailyRoutinePlan plan = planWithTwoItems();
        when(planRepository.findByUserIdAndRoutineDate(eq(1L), any()))
                .thenReturn(Optional.of(plan));

        UserRoutineContextDto context = new UserRoutineContextDto(
                "{}", "{}", EnumSet.of(RoutineCategory.DIET, RoutineCategory.EXERCISE)
        );
        when(contextClient.getContext(anyString())).thenReturn(context);
        when(openAiRoutineService.adjust(eq(context), anyList(), eq(plan.getRoutineDate()), anyString()))
                .thenReturn(new AiRoutineResultDto(
                        "일정을 확인했어요.",
                        "06:00",
                        List.of(new AiRoutineItemDto(
                                11L,
                                "DIET",
                                "저녁 식사하기",
                                "퇴근한 뒤 저녁 식사를 하세요.",
                                "퇴근 후",
                                30
                        ))
                ));

        DailyRoutineResponseDto response = routineService.adjustToday(
                1L,
                "Bearer test-token",
                adjustRequest("오늘 퇴근이 늦어")
        );

        assertEquals(2, response.getItems().size());
        assertEquals(LocalTime.of(18, 30), response.getItems().stream()
                .filter(item -> "DIET".equals(item.getCategory()))
                .findFirst()
                .orElseThrow()
                .getScheduledTime());
    }

    @Test
    void generationUsesProfileAndIgnoresOneTimeScheduleRequest() {
        DailyRoutinePlan plan = planWithTwoItems();
        plan.updateAdjustmentSummary("오늘은 19시 퇴근 일정으로 조정했어요.");
        when(planRepository.findByUserIdAndRoutineDate(eq(1L), any()))
                .thenReturn(Optional.of(plan));

        UserRoutineContextDto context = new UserRoutineContextDto(
                "{}", "{}", EnumSet.of(RoutineCategory.DIET, RoutineCategory.EXERCISE)
        );
        when(contextClient.getContext("Bearer test-token")).thenReturn(context);
        when(openAiRoutineService.generate(eq(context), isNull(), any()))
                .thenReturn(new AiRoutineResultDto(
                        "프로필의 평소 생활 패턴으로 기본 루틴을 만들었어요.",
                        "06:00",
                        List.of(
                                new AiRoutineItemDto(
                                        null, "DIET", "점심 식사", "점심을 균형 있게 드세요.",
                                        "12:30", 60
                                ),
                                new AiRoutineItemDto(
                                        null, "DIET", "저녁 식사", "저녁을 적당량 드세요.",
                                        "19:00", 60
                                ),
                                new AiRoutineItemDto(
                                        null, "EXERCISE", "저녁 운동", "퇴근 후 가볍게 걸으세요.",
                                        "20:00", 60
                                )
                        )
                ));

        GenerateRoutineRequestDto request = new GenerateRoutineRequestDto();
        request.setAdditionalRequest("오늘은 오후 7시에 퇴근할 거야");
        DailyRoutineResponseDto response = routineService.generateToday(
                1L,
                "Bearer test-token",
                request
        );

        assertEquals(2, response.getItems().size());
        assertTrue(response.getCoachMessage().startsWith(
                "프로필의 평소 생활 패턴으로 기본 루틴을 만들었어요."
        ));
        assertTrue(response.getCoachMessage().contains("이후 2개를 즉시 반영했어요."));
        assertTrue(response.getItems().stream().anyMatch(item -> "점심 식사".equals(item.getTitle())));
        verify(openAiRoutineService).generate(eq(context), isNull(), any());
    }

    @Test
    void regenerationKeepsPastItemAndChangesOnlyFutureWithinSameCount() {
        LocalDate routineDate = LocalDate.of(2026, 8, 17);
        LocalDateTime noon = LocalDateTime.of(routineDate, LocalTime.NOON);
        DailyRoutinePlan plan = DailyRoutinePlan.builder()
                .id(20L)
                .userId(1L)
                .routineDate(routineDate)
                .resetTimeSnapshot(LocalTime.MIDNIGHT)
                .build();
        DailyRoutineItem completedBreakfast = DailyRoutineItem.builder()
                .id(21L)
                .category(RoutineCategory.DIET)
                .title("기존 아침 식사")
                .description("기존에 완료한 아침 식사입니다.")
                .scheduledTime(LocalTime.of(8, 0))
                .completionWindowMinutes(30)
                .sortOrder(0)
                .build();
        completedBreakfast.changeCompleted(true);
        LocalDateTime originalCompletedAt = completedBreakfast.getCompletedAt();
        DailyRoutineItem futureExercise = DailyRoutineItem.builder()
                .id(22L)
                .category(RoutineCategory.EXERCISE)
                .title("기존 저녁 운동")
                .description("기존 미래 운동입니다.")
                .scheduledTime(LocalTime.of(18, 0))
                .completionWindowMinutes(60)
                .sortOrder(1)
                .build();
        plan.addItem(completedBreakfast);
        plan.addItem(futureExercise);

        List<DailyRoutineItem> aiItems = List.of(
                DailyRoutineItem.builder()
                        .category(RoutineCategory.DIET)
                        .title("새 점심 식사")
                        .description("새로 제안한 점심입니다.")
                        .scheduledTime(LocalTime.of(12, 30))
                        .completionWindowMinutes(60)
                        .sortOrder(0)
                        .build(),
                DailyRoutineItem.builder()
                        .category(RoutineCategory.EXERCISE)
                        .title("새 저녁 운동")
                        .description("새로 제안한 저녁 운동입니다.")
                        .scheduledTime(LocalTime.of(20, 0))
                        .completionWindowMinutes(60)
                        .sortOrder(1)
                        .build()
        );

        RoutineService.RegenerationMergeResult result = routineService.mergeRegeneratedItems(
                plan,
                plan,
                new java.util.ArrayList<>(plan.getItems()),
                aiItems,
                EnumSet.of(RoutineCategory.DIET, RoutineCategory.EXERCISE),
                noon
        );

        assertEquals(2, result.items().size());
        DailyRoutineItem preserved = result.items().stream()
                .filter(item -> "기존 아침 식사".equals(item.getTitle()))
                .findFirst()
                .orElseThrow();
        assertTrue(preserved.isCompleted());
        assertEquals(originalCompletedAt, preserved.getCompletedAt());
        assertTrue(result.items().stream().anyMatch(item -> "새 저녁 운동".equals(item.getTitle())));
        assertEquals(1, result.preservedPastCount());
        assertEquals(1, result.replacedFutureCount());
    }

    @Test
    void adjustmentDoesNotClaimSuccessWhenAllItemsAreExpired() {
        DailyRoutinePlan plan = planWithTwoItems();
        plan.getItems().forEach(DailyRoutineItem::markMissed);
        when(planRepository.findByUserIdAndRoutineDate(eq(1L), any()))
                .thenReturn(Optional.of(plan));

        DailyRoutineResponseDto response = routineService.adjustToday(
                1L,
                "Bearer test-token",
                adjustRequest("오늘은 오후 7시에 퇴근할 거야")
        );

        assertEquals(
                "변경 가능한 루틴이 없습니다. 이미 완료되었거나 마감된 항목은 수정할 수 없습니다.",
                response.getCoachMessage()
        );
        assertTrue(response.getItems().stream().anyMatch(item -> Long.valueOf(11L).equals(item.getId())));
        assertTrue(response.getItems().stream().anyMatch(item -> Long.valueOf(12L).equals(item.getId())));
        verifyNoInteractions(contextClient, openAiRoutineService);
    }

    private DailyRoutinePlan planWithTwoItems() {
        DailyRoutinePlan plan = DailyRoutinePlan.builder()
                .id(10L)
                .userId(1L)
                // 테스트 실행 시각과 무관하게 마감 잠금이 발생하지 않도록 미래 날짜를 사용한다.
                .routineDate(LocalDate.now().plusDays(1))
                .resetTimeSnapshot(LocalTime.of(6, 0))
                .build();
        plan.addItem(DailyRoutineItem.builder()
                .id(11L)
                .category(RoutineCategory.DIET)
                .title("저녁 식사하기")
                .description("균형 잡힌 저녁 식사를 하세요.")
                .scheduledTime(LocalTime.of(18, 30))
                .completionWindowMinutes(30)
                .sortOrder(0)
                .build());
        plan.addItem(DailyRoutineItem.builder()
                .id(12L)
                .category(RoutineCategory.EXERCISE)
                .title("가볍게 걷기")
                .description("20분 동안 가볍게 걸으세요.")
                .scheduledTime(LocalTime.of(20, 0))
                .completionWindowMinutes(30)
                .sortOrder(1)
                .build());
        return plan;
    }

    private AdjustRoutineRequestDto adjustRequest(String message) {
        AdjustRoutineRequestDto request = new AdjustRoutineRequestDto();
        request.setMessage(message);
        return request;
    }
}
