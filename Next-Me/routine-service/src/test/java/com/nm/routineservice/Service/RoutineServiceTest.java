package com.nm.routineservice.Service;

import com.nm.routineservice.Dto.DailyRoutineResponseDto;
import com.nm.routineservice.Dto.RoutineProgressResponseDto;
import com.nm.routineservice.Entity.DailyRoutinePlan;
import com.nm.routineservice.Entity.RoutineSetting;
import com.nm.routineservice.Repository.DailyRoutineItemRepository;
import com.nm.routineservice.Repository.DailyRoutinePlanRepository;
import com.nm.routineservice.Repository.RoutineCompletionHistoryRepository;
import com.nm.routineservice.Repository.RoutineSettingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RoutineServiceTest {

    private DailyRoutinePlanRepository planRepository;
    private RoutineService routineService;

    @BeforeEach
    void setUp() {
        RoutineSettingRepository settingRepository = mock(RoutineSettingRepository.class);
        planRepository = mock(DailyRoutinePlanRepository.class);
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
                mock(DailyRoutineItemRepository.class),
                mock(RoutineCompletionHistoryRepository.class),
                mock(UserRoutineContextClient.class),
                mock(OpenAiRoutineService.class),
                mock(CharacterRewardClient.class),
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
}
