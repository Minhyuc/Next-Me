package com.nm.routineservice.Service;

import com.nm.routineservice.Dto.AiRoutineItemDto;
import com.nm.routineservice.Dto.AiRoutineResultDto;
import com.nm.routineservice.Dto.UserRoutineContextDto;
import com.nm.routineservice.Entity.DailyRoutineItem;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
/** OpenAI Responses API로 기본 루틴과 오늘의 임시 조정안을 구조화된 JSON으로 생성한다. */
public class OpenAiRoutineService {

    private static final String INSTRUCTIONS = """
            당신은 Next-Me의 일일 루틴 코치입니다.

            기본 루틴 생성 요청인 경우:
            - profile, routine_preferences, selected_categories에 저장된 평소 생활 패턴만 사용하세요.
            - 이전 당일 루틴, 이전 일정 조정 내용, 일회성 야근·약속은 기본 루틴에 반영하지 마세요.
            - 일회성 일정 변화는 일정 조정 요청에서만 처리합니다.

            목표:
            - 사용자 프로필과 selected_categories를 근거로 오늘 실천 가능한 루틴을 만드세요.
            - 먼저 프로필에서 기상, 출근·등교, 식사, 외부 활동, 귀가, 취침으로 이어지는
              사용자의 하루 시간축을 파악한 다음 각 행동을 가장 자연스러운 시각에 배치하세요.
            - recommended_reset_time 직후부터 항목을 기계적으로 연속 배치하지 마세요.
            - 하루 전체에 수행할 행동이 있다면 아침, 낮, 저녁의 실제 생활 시각에 나누어 배치하세요.
            - 모든 행동이 아침처럼 짧은 시간대에 모이는 것이 사용자에게 명확히 최적일 때만
              집중 배치를 허용하고, 그 판단 이유를 coach_message에 설명하세요.
            - selected_categories에 없는 범주의 항목은 절대 만들지 마세요.
            - selected_categories의 모든 범주를 빠짐없이 포함하고 범주마다 최소 한 개의 항목을 만드세요.
            - 범주마다 무조건 한 개만 만드는 것이 아니라 하루 동안 실제로 필요한 횟수를 판단하세요.
            - 루틴은 전체 합계가 최대 10개가 되도록 만드세요.
            - 항목이 5개 이상이면 특별한 생활 근거가 없는 한 아침, 낮, 저녁에 최소 한 항목씩 배치하세요.
            - 각 항목은 한 번에 완료 여부를 체크할 수 있는 구체적인 행동이어야 합니다.
            - 사용자의 출퇴근, 수면, 운동, 피부 정보를 고려해 서로 겹치지 않는 현실적인 시간을 정하세요.
            - SLEEP_WAKE가 selected_categories에 있을 때만 기상과 취침 항목을 생성하세요.
            - SLEEP_WAKE가 선택되었다면 기상과 취침을 각각 별도 항목으로 반드시 만들고,
              기상 항목을 하루의 첫 항목으로, 취침 항목을 마지막 항목으로 배치하세요.
            - SLEEP_WAKE가 선택되지 않았다면 프로필의 기상·취침 정보는 다른 루틴의 적절한 시각과
              recommended_reset_time을 정하는 내부 문맥으로만 사용하고, 기상·취침 항목은 결과에 절대 넣지 마세요.
            - SLEEP_WAKE가 선택되지 않은 결과는 사용자가 선택한 범주 중 가장 이른 행동부터 시작하세요.
            - CLEANSING이 선택되었다면 세안 항목을 반드시 별도로 만들고 피부 타입에 맞는
              클렌저 유형 또는 성분, 세안할 시각, 세안 방법을 description에 구체적으로 적으세요.
            - 아침 세안과 귀가 후 또는 저녁 세안이 모두 필요하다면 서로 다른 시각의 항목으로 만드세요.
            - 화장, 자외선 차단제, 외부 활동 정보가 있으면 귀가 후 세안 필요성을 우선 검토하세요.
            - 예: 외출 후 복귀 시각부터 30분 이내에 약산성 클렌징폼으로 미지근하게 세안하세요.
            - SKIN_CARE가 선택되었다면 세안과 별도의 항목을 반드시 만들고 피부 타입과 고민에 맞는
              토너·세럼·보습제 등의 제품 유형 또는 성분, 바르는 순서, 실행 시점을 description에 적으세요.
            - 아침 보호 루틴과 저녁 회복 루틴이 모두 적합하다면 별도 항목으로 나누세요.
            - CLEANSING과 SKIN_CARE를 하나의 항목이나 다른 category로 합치지 마세요.
            - DIET가 선택되었다면 프로필의 실제 식사 패턴을 반영하되, 여러 끼를 먹는 사용자라면
              아침 한 끼만 만들지 말고 점심과 저녁을 각각 현실적인 시각에 배치하세요.
            - EXERCISE는 출퇴근·식사·수면과 충돌하지 않는 시간 중 지속하기 가장 좋은 시각을 선택하세요.
            - completion_window_minutes는 해당 시각부터 완료할 수 있는 제한 시간이며 10~180분 사이로 정하세요.
            - 세안과 스킨케어는 보통 20~40분, 운동·식사는 현실적인 수행 시간을 고려해 정하세요.
            - 의료 진단이나 처방을 하지 마세요.

            recommended_reset_time:
            - 사용자의 평소 근무·수면·기상 패턴을 분석해 하루 루틴 완료 상태를 초기화할 적합한 시각을 정하세요.
            - 초기화 시각은 하루의 새 루틴이 시작되는 경계입니다.
            - 가능하면 사용자의 주 수면이 끝나고 기상하기 직전 또는 활동을 시작하기 전의 방해가 적은 시각을 선택하세요.
            - 일회성 일정 변경이 아니라 프로필에 나타난 평소 생활 패턴을 기준으로 정하세요.
            - SLEEP_WAKE가 선택되지 않아도 프로필의 수면·기상 패턴을 참고해 첫 번째 선택 루틴보다
              방해가 적은 이른 시각을 하루 경계로 정하세요.
            - 반드시 24시간제 HH:mm 형식으로 반환하세요.

            일정 조정 요청인 경우:
            - current_items에는 서버에서 아직 완료·마감되지 않아 실제 변경 가능한 항목만 들어옵니다.
            - 사용자의 일정 변경으로 실제 영향을 받는 미완료 항목만 시간 또는 내용을 조정하세요.
            - 응답 items에는 영향을 받아 실제로 변경할 미완료 항목만 반환해도 됩니다.
            - 변경하지 않을 기존 항목은 응답에서 생략하세요. 서버가 현재 루틴을 그대로 유지합니다.
            - 기존 항목을 변경할 때는 반드시 해당 항목의 original_item_id를 반환하세요.
            - 기존 항목을 반환한다면 scheduled_time, description, completion_window_minutes 중
              적어도 하나는 현재 값과 실제로 달라야 합니다.
            - 새 항목이 꼭 필요한 경우에만 original_item_id를 null로 반환하세요.
            - 일정 변화와 충돌하는 항목이 없다면 items를 빈 배열로 반환하고 그 이유를
              coach_message에 구체적으로 설명하세요.
            - 조정 결과는 현재 routine_date 한 날짜에만 적용되는 임시 루틴입니다.
            - '오늘' 같은 상대 날짜 표현은 입력의 routine_date를 기준으로 해석하세요.
            - 사용자가 말한 시각과 프로필의 평소 출퇴근·수면 시간을 비교해 충돌하는 항목을 판단하세요.
            - 예를 들어 사용자가 '오늘 오후 7시에 퇴근해'라고 말하면 19:00 이후로 영향을 받는
              저녁 식사, 운동, 귀가 후 세안·스킨케어 등의 미완료 항목만 현실적인 시각으로 조정하세요.

            category는 CLEANSING, SKIN_CARE, SLEEP_WAKE, DIET, EXERCISE 중 하나여야 합니다.
            scheduled_time은 24시간제 HH:mm 형식이어야 합니다.
            coach_message에는 생성 또는 조정 이유를 한국어로 간단히 설명하세요.
            """;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String model;

    public OpenAiRoutineService(
            @Value("${openai.api-key}") String apiKey,
            @Value("${openai.model}") String model,
            @Value("${openai.endpoint}") String endpoint,
            ObjectMapper objectMapper
    ) {
        this.model = model;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
                .baseUrl(endpoint)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    public AiRoutineResultDto generate(
            UserRoutineContextDto context,
            String additionalRequest,
            ZonedDateTime currentDateTime
    ) {
        // 프로필, 선호도, 현재 시각을 함께 전달해 현실적인 시간표와 초기화 시각을 받는다.
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("mode", "GENERATE");
        input.put("profile", readJson(context.getProfileJson()));
        input.put("routine_preferences", readJson(context.getPreferenceJson()));
        input.put("selected_categories", context.getSelectedCategories().stream().map(Enum::name).toList());
        input.put("current_datetime", currentDateTime.toString());
        input.put("additional_request", additionalRequest == null ? "" : additionalRequest);
        AiRoutineResultDto result = callOpenAi(input);
        Set<String> missing = findMissingCategories(context, result);
        Set<String> insufficientDetails = findInsufficientSkinCareDetails(context, result);
        Set<String> coverageConcerns = findDailyCoverageConcerns(context, result);
        // 선택 범주, 피부 관리 정보, 하루 분포가 부족하면 AI가 전체 시간축을 한 번 더 검토한다.
        if (!missing.isEmpty() || !insufficientDetails.isEmpty() || !coverageConcerns.isEmpty()) {
            input.put("required_missing_categories", missing);
            input.put("required_detailed_categories", insufficientDetails);
            input.put("daily_coverage_concerns", coverageConcerns);
            input.put("correction", "루틴은 최대 10개로 제한하고 사용자의 기상부터 취침까지 하루 전체 시간축을 다시 검토하세요. 누락 범주와 기상·취침을 보완하고, 세안·스킨케어에는 제품 유형 또는 성분을 구체적으로 적으세요. 항목이 5개 이상이면 아침·낮·저녁에 최소 한 항목씩 배치하고, 여러 끼의 식사와 귀가 후 관리가 필요하면 별도 항목으로 나누세요.");
            result = callOpenAi(input);
        }
        ensureAllSelectedCategories(context, result);
        ensureSkinCareDetails(context, result);
        ensureRequiredDailyCoverage(context, result);
        return result;
    }

    public AiRoutineResultDto adjust(
            UserRoutineContextDto context,
            List<DailyRoutineItem> currentItems,
            LocalDate routineDate,
            String changeRequest
    ) {
        // original_item_id를 통해 조정 전후 항목과 완료 상태를 연결한다.
        List<Map<String, Object>> items = currentItems.stream().map(item -> {
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("id", item.getId());
            value.put("category", item.getCategory().name());
            value.put("title", item.getTitle());
            value.put("description", item.getDescription());
            value.put("scheduled_time", item.getScheduledTime().toString());
            value.put("completion_window_minutes", item.getCompletionWindowMinutes());
            value.put("completed", item.isCompleted());
            return value;
        }).toList();

        Map<String, Object> input = new LinkedHashMap<>();
        input.put("mode", "ADJUST");
        input.put("profile", readJson(context.getProfileJson()));
        input.put("routine_preferences", readJson(context.getPreferenceJson()));
        input.put("selected_categories", context.getSelectedCategories().stream().map(Enum::name).toList());
        input.put("routine_date", routineDate.toString());
        input.put("current_items", items);
        input.put("user_schedule_change", changeRequest);
        AiRoutineResultDto result = callOpenAi(input);
        Set<String> insufficientDetails = findInsufficientReturnedSkinCareDetails(result);
        if (!insufficientDetails.isEmpty()) {
            input.put("required_detailed_categories", insufficientDetails);
            input.put("correction", "영향받는 항목만 반환하되, 변경하는 세안·스킨케어 항목에는 제품 유형 또는 성분과 실행 방법을 구체적으로 적어 다시 반환하세요.");
            result = callOpenAi(input);
        }
        Set<String> remainingInsufficient = findInsufficientReturnedSkinCareDetails(result);
        if (!remainingInsufficient.isEmpty()) {
            throw new IllegalStateException(
                    "AI 피부 관리 루틴의 제품·성분 정보가 부족합니다: " + remainingInsufficient
            );
        }
        return result;
    }

    private AiRoutineResultDto callOpenAi(Map<String, Object> input) {
        JsonNode response;
        try {
            response = restClient.post()
                    .uri("/responses")
                    .body(createRequestBody(input))
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException exception) {
            throw new IllegalStateException("AI 루틴 생성 요청에 실패했습니다.", exception);
        }

        return parseResult(extractOutputText(response));
    }

    private Map<String, Object> createRequestBody(Map<String, Object> input) {
        // Structured Outputs로 시간, 카테고리, 제한 시간 필드를 항상 받는다.
        Map<String, Object> itemProperties = new LinkedHashMap<>();
        itemProperties.put("original_item_id", Map.of("type", List.of("integer", "null")));
        itemProperties.put("category", Map.of(
                "type", "string",
                "enum", List.of("CLEANSING", "SKIN_CARE", "SLEEP_WAKE", "DIET", "EXERCISE")
        ));
        itemProperties.put("title", Map.of("type", "string"));
        itemProperties.put("description", Map.of("type", "string"));
        itemProperties.put("scheduled_time", Map.of(
                "type", "string",
                "pattern", "^([01]\\d|2[0-3]):[0-5]\\d$"
        ));
        itemProperties.put("completion_window_minutes", Map.of(
                "type", "integer", "minimum", 10, "maximum", 180
        ));

        Map<String, Object> itemSchema = new LinkedHashMap<>();
        itemSchema.put("type", "object");
        itemSchema.put("properties", itemProperties);
        itemSchema.put("required", List.of(
                "original_item_id", "category", "title", "description", "scheduled_time",
                "completion_window_minutes"
        ));
        itemSchema.put("additionalProperties", false);

        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("coach_message", Map.of("type", "string"));
        properties.put("recommended_reset_time", Map.of(
                "type", "string",
                "pattern", "^([01]\\d|2[0-3]):[0-5]\\d$"
        ));
        properties.put("items", Map.of(
                "type", "array",
                "items", itemSchema,
                "maxItems", 10
        ));

        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", List.of("coach_message", "recommended_reset_time", "items"));
        schema.put("additionalProperties", false);

        Map<String, Object> format = new LinkedHashMap<>();
        format.put("type", "json_schema");
        format.put("name", "daily_routine_plan");
        format.put("schema", schema);
        format.put("strict", true);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("instructions", INSTRUCTIONS);
        body.put("input", objectMapper.writeValueAsString(input));
        // 모델마다 지원 여부가 다른 reasoning/verbosity 옵션은 사용하지 않는다.
        body.put("text", Map.of("format", format));
        return body;
    }

    private JsonNode readJson(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (JacksonException exception) {
            throw new IllegalStateException("사용자 정보를 처리할 수 없습니다.", exception);
        }
    }

    private String extractOutputText(JsonNode response) {
        if (response == null || !response.has("output")) {
            throw new IllegalStateException("OpenAI에서 올바른 응답을 받지 못했습니다.");
        }

        StringBuilder outputText = new StringBuilder();
        response.path("output").forEach(item -> item.path("content").forEach(content -> {
            if ("output_text".equals(content.path("type").asText())) {
                outputText.append(content.path("text").asText());
            }
        }));
        if (outputText.isEmpty()) {
            throw new IllegalStateException("OpenAI 응답에 루틴이 없습니다.");
        }
        return outputText.toString();
    }

    private AiRoutineResultDto parseResult(String outputText) {
        try {
            JsonNode result = objectMapper.readTree(outputText);
            List<AiRoutineItemDto> items = new ArrayList<>();
            result.path("items").forEach(item -> items.add(new AiRoutineItemDto(
                    item.path("original_item_id").isNull() ? null : item.path("original_item_id").longValue(),
                    item.path("category").asText(),
                    item.path("title").asText(),
                    item.path("description").asText(),
                    item.path("scheduled_time").asText(),
                    item.path("completion_window_minutes").asInt(30)
            )));
            return new AiRoutineResultDto(
                    result.path("coach_message").asText(),
                    result.path("recommended_reset_time").asText(),
                    items
            );
        } catch (JacksonException exception) {
            throw new IllegalStateException("OpenAI 루틴 응답을 처리할 수 없습니다.", exception);
        }
    }

    private Set<String> findMissingCategories(
            UserRoutineContextDto context,
            AiRoutineResultDto result
    ) {
        Set<String> actual = new HashSet<>();
        result.getItems().forEach(item -> actual.add(item.getCategory()));
        Set<String> missing = new HashSet<>();
        context.getSelectedCategories().forEach(category -> {
            if (!actual.contains(category.name())) {
                missing.add(category.name());
            }
        });
        return missing;
    }

    private void ensureAllSelectedCategories(
            UserRoutineContextDto context,
            AiRoutineResultDto result
    ) {
        Set<String> missing = findMissingCategories(context, result);
        if (!missing.isEmpty()) {
            throw new IllegalStateException("AI 루틴에서 선택 범주가 누락되었습니다: " + missing);
        }
    }

    private void ensureSkinCareDetails(
            UserRoutineContextDto context,
            AiRoutineResultDto result
    ) {
        Set<String> insufficient = findInsufficientSkinCareDetails(context, result);
        if (!insufficient.isEmpty()) {
            throw new IllegalStateException("AI 피부 관리 루틴의 제품·성분 정보가 부족합니다: " + insufficient);
        }
    }

    private Set<String> findInsufficientSkinCareDetails(
            UserRoutineContextDto context,
            AiRoutineResultDto result
    ) {
        Set<String> insufficient = new HashSet<>();
        if (context.getSelectedCategories().stream().anyMatch(category -> category.name().equals("CLEANSING"))
                && !hasDetailedSkinCareItem(result, "CLEANSING", List.of(
                "클렌저", "클렌징폼", "세안제", "클렌징 오일", "클렌징 워터", "약산성"
        ))) {
            insufficient.add("CLEANSING");
        }
        if (context.getSelectedCategories().stream().anyMatch(category -> category.name().equals("SKIN_CARE"))
                && !hasDetailedSkinCareItem(result, "SKIN_CARE", List.of(
                "토너", "세럼", "앰플", "에센스", "로션", "보습제", "크림", "선크림", "자외선", "성분"
        ))) {
            insufficient.add("SKIN_CARE");
        }
        return insufficient;
    }

    private Set<String> findInsufficientReturnedSkinCareDetails(AiRoutineResultDto result) {
        Set<String> returnedCategories = new HashSet<>();
        result.getItems().forEach(item -> returnedCategories.add(item.getCategory()));

        Set<String> insufficient = new HashSet<>();
        if (returnedCategories.contains("CLEANSING")
                && !hasDetailedSkinCareItem(result, "CLEANSING", List.of(
                "클렌저", "클렌징폼", "세안제", "클렌징 오일", "클렌징 워터", "약산성"
        ))) {
            insufficient.add("CLEANSING");
        }
        if (returnedCategories.contains("SKIN_CARE")
                && !hasDetailedSkinCareItem(result, "SKIN_CARE", List.of(
                "토너", "세럼", "앰플", "에센스", "로션", "보습제", "크림", "선크림", "자외선", "성분"
        ))) {
            insufficient.add("SKIN_CARE");
        }
        return insufficient;
    }

    Set<String> findDailyCoverageConcerns(
            UserRoutineContextDto context,
            AiRoutineResultDto result
    ) {
        Set<String> concerns = new HashSet<>();
        Set<String> selected = new HashSet<>();
        context.getSelectedCategories().forEach(category -> selected.add(category.name()));

        if (selected.contains("SLEEP_WAKE")) {
            if (!hasItemContainingAny(result, List.of("기상", "일어나", "잠에서 깨"))) {
                concerns.add("SLEEP_WAKE_MISSING_WAKE_UP");
            }
            if (!hasItemContainingAny(result, List.of("취침", "잠자리", "잠들", "수면 시작"))) {
                concerns.add("SLEEP_WAKE_MISSING_BEDTIME");
            }
        }
        if (selected.contains("DIET") && countCategory(result, "DIET") < 2) {
            concerns.add("REVIEW_SINGLE_DIET_ITEM");
        }
        if (selected.contains("CLEANSING") && countCategory(result, "CLEANSING") < 2) {
            concerns.add("REVIEW_SINGLE_CLEANSING_ITEM");
        }
        if (selected.contains("SKIN_CARE") && countCategory(result, "SKIN_CARE") < 2) {
            concerns.add("REVIEW_SINGLE_SKIN_CARE_ITEM");
        }
        if (isPotentiallyOverClustered(result)) {
            concerns.add("ROUTINES_CLUSTERED_IN_SHORT_TIME_WINDOW");
        }
        if (result.getItems().size() > 10) {
            concerns.add("TOO_MANY_ROUTINE_ITEMS");
        }
        if (result.getItems().size() >= 5) {
            if (!hasItemInPeriod(result, 4, 12)) {
                concerns.add("MISSING_MORNING_PERIOD");
            }
            if (!hasItemInPeriod(result, 12, 17)) {
                concerns.add("MISSING_DAYTIME_PERIOD");
            }
            if (!hasEveningItem(result)) {
                concerns.add("MISSING_EVENING_PERIOD");
            }
        }
        return concerns;
    }

    private void ensureRequiredDailyCoverage(
            UserRoutineContextDto context,
            AiRoutineResultDto result
    ) {
        Set<String> requiredMissing = new HashSet<>(findDailyCoverageConcerns(context, result));
        requiredMissing.removeIf(concern -> concern.startsWith("REVIEW_")
                || "ROUTINES_CLUSTERED_IN_SHORT_TIME_WINDOW".equals(concern));
        if (!requiredMissing.isEmpty()) {
            throw new IllegalStateException("AI 루틴의 필수 하루 흐름이 누락되었습니다: " + requiredMissing);
        }
    }

    private int countCategory(AiRoutineResultDto result, String category) {
        return (int) result.getItems().stream()
                .filter(item -> category.equals(item.getCategory()))
                .count();
    }

    private boolean hasItemInPeriod(AiRoutineResultDto result, int startHour, int endHour) {
        return result.getItems().stream().anyMatch(item -> {
            try {
                int hour = java.time.LocalTime.parse(item.getScheduledTime()).getHour();
                return hour >= startHour && hour < endHour;
            } catch (RuntimeException exception) {
                return false;
            }
        });
    }

    private boolean hasEveningItem(AiRoutineResultDto result) {
        return result.getItems().stream().anyMatch(item -> {
            try {
                int hour = java.time.LocalTime.parse(item.getScheduledTime()).getHour();
                return hour >= 17 || hour < 4;
            } catch (RuntimeException exception) {
                return false;
            }
        });
    }

    private boolean hasItemContainingAny(AiRoutineResultDto result, List<String> keywords) {
        return result.getItems().stream().anyMatch(item -> {
            String value = (item.getTitle() == null ? "" : item.getTitle()) + " "
                    + (item.getDescription() == null ? "" : item.getDescription());
            return keywords.stream().anyMatch(value::contains);
        });
    }

    private boolean isPotentiallyOverClustered(AiRoutineResultDto result) {
        if (result.getItems().size() < 3) {
            return false;
        }
        try {
            java.time.LocalTime resetTime = java.time.LocalTime.parse(
                    result.getRecommendedResetTime()
            );
            List<Integer> offsets = result.getItems().stream()
                    .map(AiRoutineItemDto::getScheduledTime)
                    .map(java.time.LocalTime::parse)
                    .map(time -> {
                        int difference = time.toSecondOfDay() - resetTime.toSecondOfDay();
                        return difference < 0 ? difference + 24 * 60 * 60 : difference;
                    })
                    .sorted()
                    .toList();
            int spanSeconds = offsets.get(offsets.size() - 1) - offsets.get(0);
            return spanSeconds < 6 * 60 * 60;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private boolean hasDetailedSkinCareItem(
            AiRoutineResultDto result,
            String category,
            List<String> productKeywords
    ) {
        return result.getItems().stream()
                .filter(item -> category.equals(item.getCategory()))
                .map(AiRoutineItemDto::getDescription)
                .filter(description -> description != null && description.trim().length() >= 15)
                .anyMatch(description -> productKeywords.stream().anyMatch(description::contains));
    }
}
