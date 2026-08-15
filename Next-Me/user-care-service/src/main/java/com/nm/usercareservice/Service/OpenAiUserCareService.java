package com.nm.usercareservice.Service;

import com.nm.usercareservice.Dto.AiUserCareResultDto;
import com.nm.usercareservice.Dto.CareIngredientDto;
import com.nm.usercareservice.Dto.UserCareProfileContextDto;
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

@Service
/** 피부 프로필을 바탕으로 추천 성분 3개와 주의 성분 3개를 구조화해서 생성한다. */
public class OpenAiUserCareService {

    private static final String INSTRUCTIONS = """
            당신은 Next:Me의 화장품 성분 기반 피부 관리 코치입니다.

            제공된 사용자 프로필에서 확인 가능한 피부 타입과 고민만 사용하세요.
            추천 성분은 정확히 3개, 주의 성분은 정확히 3개를 한국어로 작성하세요.
            각 목록에는 세안(CLEANSING)과 피부 미용·기초 관리(SKIN_CARE)가 최소 한 번씩 포함되어야 합니다.
            특정 브랜드나 구매 링크를 추천하지 말고 제품 유형과 대표 성분을 안내하세요.
            주의 성분은 모든 사람에게 무조건 해롭다고 단정하지 말고, 현재 피부 타입이나 고민에서 농도·빈도·병용에 주의해야 하는 이유를 설명하세요.
            처방약을 추천하거나 질환을 진단하지 마세요.
            심한 염증, 통증, 출혈, 알레르기 반응이 있으면 피부과 전문의 상담이 필요하다고 안내하세요.
            새 제품은 국소 부위 패치 테스트 후 사용할 것을 disclaimer에 포함하세요.
            프로필에 없는 알레르기, 질환, 사용 제품을 추측하지 마세요.
            """;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String model;

    public OpenAiUserCareService(
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

    public AiUserCareResultDto recommend(UserCareProfileContextDto context) {
        JsonNode response;
        try {
            response = restClient.post()
                    .uri("/responses")
                    .body(createRequestBody(context.profileJson()))
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException exception) {
            throw new IllegalStateException("AI 피부 관리 성분 추천 요청에 실패했습니다.", exception);
        }
        return parseResult(extractOutputText(response));
    }

    private Map<String, Object> createRequestBody(String profileJson) {
        Map<String, Object> itemProperties = new LinkedHashMap<>();
        itemProperties.put("care_area", Map.of(
                "type", "string", "enum", List.of("CLEANSING", "SKIN_CARE")
        ));
        itemProperties.put("product_type", Map.of("type", "string"));
        itemProperties.put("ingredient", Map.of("type", "string"));
        itemProperties.put("reason", Map.of("type", "string"));
        itemProperties.put("guidance", Map.of("type", "string"));
        Map<String, Object> itemSchema = objectSchema(
                itemProperties,
                List.of("care_area", "product_type", "ingredient", "reason", "guidance")
        );

        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("summary", Map.of("type", "string"));
        properties.put("recommended_ingredients", fixedThreeArray(itemSchema));
        properties.put("caution_ingredients", fixedThreeArray(itemSchema));
        properties.put("disclaimer", Map.of("type", "string"));

        Map<String, Object> schema = objectSchema(
                properties,
                List.of("summary", "recommended_ingredients", "caution_ingredients", "disclaimer")
        );
        Map<String, Object> format = new LinkedHashMap<>();
        format.put("type", "json_schema");
        format.put("name", "user_care_ingredient_recommendation");
        format.put("schema", schema);
        format.put("strict", true);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("instructions", INSTRUCTIONS);
        body.put("input", "사용자 피부 관련 프로필(JSON): " + profileJson);
        body.put("text", Map.of("format", format));
        return body;
    }

    private Map<String, Object> fixedThreeArray(Map<String, Object> itemSchema) {
        return Map.of(
                "type", "array",
                "minItems", 3,
                "maxItems", 3,
                "items", itemSchema
        );
    }

    private Map<String, Object> objectSchema(
            Map<String, Object> properties,
            List<String> required
    ) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", required);
        schema.put("additionalProperties", false);
        return schema;
    }

    private AiUserCareResultDto parseResult(String outputText) {
        try {
            JsonNode root = objectMapper.readTree(outputText);
            List<CareIngredientDto> recommended = parseItems(root.path("recommended_ingredients"));
            List<CareIngredientDto> caution = parseItems(root.path("caution_ingredients"));
            if (recommended.size() != 3 || caution.size() != 3) {
                throw new IllegalStateException("AI가 성분을 각각 세 가지씩 반환하지 않았습니다.");
            }
            return new AiUserCareResultDto(
                    root.path("summary").asText("").trim(),
                    recommended,
                    caution,
                    root.path("disclaimer").asText("").trim()
            );
        } catch (JacksonException exception) {
            throw new IllegalStateException("AI 피부 관리 추천 응답을 처리할 수 없습니다.", exception);
        }
    }

    private List<CareIngredientDto> parseItems(JsonNode arrayNode) {
        List<CareIngredientDto> items = new ArrayList<>();
        arrayNode.forEach(item -> items.add(CareIngredientDto.builder()
                .careArea(item.path("care_area").asText())
                .productType(item.path("product_type").asText())
                .ingredient(item.path("ingredient").asText())
                .reason(item.path("reason").asText())
                .guidance(item.path("guidance").asText())
                .build()));
        return items;
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
            throw new IllegalStateException("OpenAI 응답에 성분 추천 결과가 없습니다.");
        }
        return outputText.toString();
    }
}
