package com.nm.routineservice.Service;

import com.nm.routineservice.Dto.AiRoutineItemDto;
import com.nm.routineservice.Dto.AiRoutineResultDto;
import com.nm.routineservice.Dto.UserRoutineContextDto;
import com.nm.routineservice.Entity.RoutineCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenAiRoutineServiceTest {

    private OpenAiRoutineService openAiRoutineService;
    private UserRoutineContextDto allCategoryContext;

    @BeforeEach
    void setUp() {
        openAiRoutineService = new OpenAiRoutineService(
                "test-api-key",
                "test-model",
                "https://api.openai.com/v1",
                new ObjectMapper()
        );
        allCategoryContext = new UserRoutineContextDto(
                "{}",
                "{}",
                EnumSet.of(
                        RoutineCategory.CLEANSING,
                        RoutineCategory.SKIN_CARE,
                        RoutineCategory.SLEEP_WAKE,
                        RoutineCategory.DIET,
                        RoutineCategory.EXERCISE
                )
        );
    }

    @Test
    void detectsMorningOnlyRoutineAndMissingBedtime() {
        AiRoutineResultDto morningOnly = new AiRoutineResultDto(
                "아침 루틴입니다.",
                "06:30",
                List.of(
                        item("SLEEP_WAKE", "기상", "06:30"),
                        item("CLEANSING", "아침 세안", "06:45"),
                        item("SKIN_CARE", "아침 스킨케어", "07:15"),
                        item("DIET", "아침 식사", "07:55"),
                        item("EXERCISE", "운동", "09:00")
                )
        );

        Set<String> concerns = openAiRoutineService.findDailyCoverageConcerns(
                allCategoryContext,
                morningOnly
        );

        assertTrue(concerns.contains("SLEEP_WAKE_MISSING_BEDTIME"));
        assertTrue(concerns.contains("REVIEW_SINGLE_DIET_ITEM"));
        assertTrue(concerns.contains("REVIEW_SINGLE_CLEANSING_ITEM"));
        assertTrue(concerns.contains("REVIEW_SINGLE_SKIN_CARE_ITEM"));
        assertTrue(concerns.contains("ROUTINES_CLUSTERED_IN_SHORT_TIME_WINDOW"));
        assertTrue(concerns.contains("MISSING_DAYTIME_PERIOD"));
        assertTrue(concerns.contains("MISSING_EVENING_PERIOD"));
    }

    @Test
    void acceptsRoutineDistributedAcrossTheWholeDay() {
        AiRoutineResultDto distributed = new AiRoutineResultDto(
                "하루 생활 흐름에 맞춰 배치했습니다.",
                "06:30",
                List.of(
                        item("SLEEP_WAKE", "기상", "06:30"),
                        item("CLEANSING", "아침 세안", "06:45"),
                        item("SKIN_CARE", "아침 스킨케어", "07:10"),
                        item("DIET", "아침 식사", "07:40"),
                        item("DIET", "점심 식사", "12:30"),
                        item("EXERCISE", "퇴근 후 운동", "18:30"),
                        item("DIET", "저녁 식사", "19:30"),
                        item("CLEANSING", "귀가 후 세안", "20:30"),
                        item("SKIN_CARE", "저녁 스킨케어", "21:00"),
                        item("SLEEP_WAKE", "취침", "23:30")
                )
        );

        Set<String> concerns = openAiRoutineService.findDailyCoverageConcerns(
                allCategoryContext,
                distributed
        );

        assertTrue(concerns.isEmpty());
    }

    @Test
    void rejectsMoreThanTenRoutineItems() {
        AiRoutineResultDto tooMany = new AiRoutineResultDto(
                "루틴이 너무 많습니다.",
                "06:30",
                List.of(
                        item("SLEEP_WAKE", "기상", "06:30"),
                        item("CLEANSING", "아침 세안", "06:45"),
                        item("SKIN_CARE", "아침 스킨케어", "07:10"),
                        item("DIET", "아침 식사", "07:40"),
                        item("DIET", "간식", "10:00"),
                        item("DIET", "점심 식사", "12:30"),
                        item("EXERCISE", "오후 운동", "16:00"),
                        item("DIET", "저녁 식사", "19:00"),
                        item("CLEANSING", "저녁 세안", "20:00"),
                        item("SKIN_CARE", "저녁 스킨케어", "21:00"),
                        item("SLEEP_WAKE", "취침", "23:30")
                )
        );

        Set<String> concerns = openAiRoutineService.findDailyCoverageConcerns(
                allCategoryContext,
                tooMany
        );

        assertTrue(concerns.contains("TOO_MANY_ROUTINE_ITEMS"));
    }

    private AiRoutineItemDto item(String category, String title, String scheduledTime) {
        return new AiRoutineItemDto(
                null,
                category,
                title,
                "구체적인 실행 방법을 안내합니다.",
                scheduledTime,
                30
        );
    }
}
