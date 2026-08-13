package com.nm.personalcolorservice.Service;

import com.nm.personalcolorservice.Dto.AiPersonalColorAnalysisResultDto;
import com.nm.personalcolorservice.Entity.PersonalColorMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
/** OpenAI Responses API를 호출해 다음 질문 또는 최종 퍼스널컬러를 구조화된 JSON으로 받는다. */
public class OpenAiPersonalColorAnalysisService {

    private static final Set<String> ALLOWED_COLORS = Set.of(
            "봄 웜", "여름 쿨", "가을 웜", "겨울 쿨"
    );

    private static final String INSTRUCTIONS = """
            당신은 Next-Me의 퍼스널컬러 진단 코치입니다.
            선택지를 한 번에 제시하는 설문이 아니라 자연스러운 한국어 대화를 진행하세요.

            규칙:
            - 사용자의 프로필과 지금까지의 대화를 참고하세요.
            - 프로필에 기존 personalColor가 있더라도 이전 판정값은 근거로 사용하지 말고 현재 대화 내용으로 새롭게 판정하세요.
            - 피부의 명도와 채도, 햇빛 반응, 잘 어울리는 옷 색, 액세서리 색 등 필요한 정보를 한 번에 하나씩 질문하세요.
            - 사용자가 모르는 항목은 억지로 답하게 하지 말고 다른 관찰 질문으로 전환하세요.
            - 정보가 부족하면 completed=false로 두고 reply에는 다음 질문 하나만 작성하세요.
            - 정보가 충분하면 봄 웜, 여름 쿨, 가을 웜, 겨울 쿨 중 하나를 판정하세요.
            - 사진을 직접 분석한 것처럼 말하지 마세요.
            - 이 결과는 전문 색채 진단을 대체하지 않는 간이 AI 추천임을 완료 답변에 짧게 알리세요.
            """;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String model;

    public OpenAiPersonalColorAnalysisService(
            ObjectMapper objectMapper,
            @Value("${openai.api-key}") String apiKey,
            @Value("${openai.model}") String model,
            @Value("${openai.endpoint}") String endpoint
    ) {
        this.objectMapper = objectMapper;
        this.model = model;
        this.restClient = RestClient.builder()
                .baseUrl(endpoint)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    public AiPersonalColorAnalysisResultDto analyze(
            String profileJson,
            List<PersonalColorMessage> messages
    ) {
        // 프로필과 지금까지의 대화를 함께 보내야 AI가 같은 질문을 반복하지 않는다.
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("profile", readJson(profileJson));
        input.put("conversation", toConversation(messages));

        JsonNode response;
        try {
            response = restClient.post()
                    .uri("/responses")
                    .body(createRequestBody(input))
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException exception) {
            throw new IllegalStateException("AI 퍼스널컬러 대화 요청에 실패했습니다.", exception);
        }
        return parseResult(extractOutputText(response));
    }

    private Map<String, Object> createRequestBody(Map<String, Object> input) {
        // Structured Outputs 스키마로 응답 필드와 허용 범위를 강제한다.
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("reply", Map.of("type", "string"));
        properties.put("completed", Map.of("type", "boolean"));
        properties.put("personal_color", Map.of(
                "type", "string",
                "enum", List.of("", "봄 웜", "여름 쿨", "가을 웜", "겨울 쿨")
        ));
        properties.put("confidence", Map.of("type", "integer", "minimum", 0, "maximum", 100));

        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", List.of("reply", "completed", "personal_color", "confidence"));
        schema.put("additionalProperties", false);

        Map<String, Object> format = new LinkedHashMap<>();
        format.put("type", "json_schema");
        format.put("name", "personal_color_analysis");
        format.put("schema", schema);
        format.put("strict", true);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("instructions", INSTRUCTIONS);
        body.put("input", objectMapper.writeValueAsString(input));
        // 모델별 선택 옵션을 제외해 기본 모델(gpt-4o-mini)과 호환되게 한다.
        body.put("text", Map.of("format", format));
        return body;
    }

    private AiPersonalColorAnalysisResultDto parseResult(String outputText) {
        // 완료 전에는 personalColor/confidence를 null로 바꿔 프론트 상태 판단을 단순화한다.
        try {
            JsonNode root = objectMapper.readTree(outputText);
            boolean completed = root.path("completed").asBoolean(false);
            String personalColor = root.path("personal_color").asText("").trim();
            int confidence = root.path("confidence").asInt(0);
            if (completed && !ALLOWED_COLORS.contains(personalColor)) {
                throw new IllegalStateException("AI가 올바르지 않은 퍼스널컬러를 반환했습니다.");
            }
            return new AiPersonalColorAnalysisResultDto(
                    root.path("reply").asText(),
                    completed,
                    completed ? personalColor : null,
                    completed ? confidence : null
            );
        } catch (JacksonException exception) {
            throw new IllegalStateException("AI 퍼스널컬러 대화 응답을 처리할 수 없습니다.", exception);
        }
    }

    private List<Map<String, String>> toConversation(List<PersonalColorMessage> messages) {
        List<Map<String, String>> conversation = new ArrayList<>();
        messages.forEach(message -> conversation.add(Map.of(
                "role", message.getRole(),
                "content", message.getContent()
        )));
        return conversation;
    }

    private JsonNode readJson(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (JacksonException exception) {
            throw new IllegalStateException("사용자 프로필을 처리할 수 없습니다.", exception);
        }
    }

    private String extractOutputText(JsonNode response) {
        // Responses API의 output 배열 중 실제 output_text 조각만 합친다.
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
            throw new IllegalStateException("OpenAI 응답에 대화 결과가 없습니다.");
        }
        return outputText.toString();
    }
}
