package com.nm.personalcolorservice.Service;

import com.nm.personalcolorservice.Dto.AiPersonalColorResultDto;
import com.nm.personalcolorservice.Dto.CosmeticRecommendationDto;
import com.nm.personalcolorservice.Dto.OutfitRecommendationDto;
import com.nm.personalcolorservice.Dto.PersonalColorRecommendationRequestDto;
import com.nm.personalcolorservice.Dto.UserPersonalColorContextDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
/** 프로필 퍼스널컬러와 자연어 요청을 바탕으로 옷 또는 화장 추천을 생성한다. */
public class OpenAiPersonalColorService {

    private static final String COMMON_INSTRUCTIONS = """
            당신은 Next-Me의 퍼스널컬러 스타일 코치입니다.

            규칙:
            - 특정 브랜드나 구매 링크가 아니라 일반적인 제품 유형과 색상으로 추천하세요.
            - 날씨가 제공되지 않았다면 실제 날씨를 추측하지 말고 날짜의 계절감과 입력된 상황만 사용하세요.
            - 사용자가 입력하지 않은 신체 정보, 소유 제품, 실제 기상 상태를 지어내지 마세요.
            """;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String model;

    public OpenAiPersonalColorService(
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

    public AiPersonalColorResultDto recommendOutfits(
            UserPersonalColorContextDto context,
            PersonalColorRecommendationRequestDto request,
            LocalDate recommendationDate
    ) {
        // 화장 배열은 비우고 옷 배열만 생성하도록 스키마를 구성한다.
        return recommend(context, request, recommendationDate, false, true);
    }

    public AiPersonalColorResultDto recommendMakeup(
            UserPersonalColorContextDto context,
            PersonalColorRecommendationRequestDto request,
            LocalDate recommendationDate
    ) {
        // 옷 배열은 비우고 화장 배열만 생성하도록 스키마를 구성한다.
        return recommend(context, request, recommendationDate, true, false);
    }

    private AiPersonalColorResultDto recommend(
            UserPersonalColorContextDto context,
            PersonalColorRecommendationRequestDto request,
            LocalDate recommendationDate,
            boolean includeMakeup,
            boolean includeOutfits
    ) {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("recommendation_date", recommendationDate.toString());
        input.put("profile", readJson(context.profileJson()));
        input.put("personal_color", context.personalColor());
        input.put("message", request.getMessage().trim());

        JsonNode response;
        try {
            response = restClient.post()
                    .uri("/responses")
                    .body(createRequestBody(input, includeMakeup, includeOutfits))
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException exception) {
            throw new IllegalStateException("AI 퍼스널컬러 추천 요청에 실패했습니다.", exception);
        }
        return parseResult(extractOutputText(response));
    }

    private Map<String, Object> createRequestBody(
            Map<String, Object> input,
            boolean includeMakeup,
            boolean includeOutfits
    ) {
        // 두 추천 형식은 같은 최상위 구조를 쓰되 min/maxItems로 필요한 한쪽만 허용한다.
        Map<String, Object> cosmeticProperties = new LinkedHashMap<>();
        cosmeticProperties.put("category", Map.of("type", "string"));
        cosmeticProperties.put("product_type", Map.of("type", "string"));
        cosmeticProperties.put("recommended_colors", Map.of(
                "type", "array", "items", Map.of("type", "string")
        ));
        cosmeticProperties.put("finish", Map.of("type", "string"));
        cosmeticProperties.put("reason", Map.of("type", "string"));

        Map<String, Object> cosmeticSchema = objectSchema(
                cosmeticProperties,
                List.of("category", "product_type", "recommended_colors", "finish", "reason")
        );

        Map<String, Object> outfitProperties = new LinkedHashMap<>();
        outfitProperties.put("style", Map.of("type", "string"));
        outfitProperties.put("top", Map.of("type", "string"));
        outfitProperties.put("bottom", Map.of("type", "string"));
        outfitProperties.put("outerwear", Map.of("type", "string"));
        outfitProperties.put("shoes", Map.of("type", "string"));
        outfitProperties.put("accessories", Map.of("type", "string"));
        outfitProperties.put("color_palette", Map.of(
                "type", "array", "items", Map.of("type", "string")
        ));
        outfitProperties.put("reason", Map.of("type", "string"));

        Map<String, Object> outfitSchema = objectSchema(
                outfitProperties,
                List.of("style", "top", "bottom", "outerwear", "shoes", "accessories",
                        "color_palette", "reason")
        );

        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("summary", Map.of("type", "string"));
        properties.put("cosmetics", Map.of(
                "type", "array", "minItems", includeMakeup ? 3 : 0,
                "maxItems", includeMakeup ? 10 : 0, "items", cosmeticSchema
        ));
        properties.put("outfits", Map.of(
                "type", "array", "minItems", includeOutfits ? 1 : 0,
                "maxItems", includeOutfits ? 10 : 0, "items", outfitSchema
        ));

        Map<String, Object> schema = objectSchema(
                properties, List.of("summary", "cosmetics", "outfits")
        );
        Map<String, Object> format = new LinkedHashMap<>();
        format.put("type", "json_schema");
        format.put("name", "personal_color_recommendation");
        format.put("schema", schema);
        format.put("strict", true);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("instructions", buildInstructions(includeMakeup, includeOutfits));
        body.put("input", objectMapper.writeValueAsString(input));
        // 기본 모델을 포함한 여러 모델에서 공통 지원되는 JSON Schema 설정만 사용한다.
        body.put("text", Map.of("format", format));
        return body;
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

    private AiPersonalColorResultDto parseResult(String outputText) {
        // AI의 snake_case JSON을 프론트 응답용 camelCase DTO로 변환한다.
        try {
            JsonNode root = objectMapper.readTree(outputText);
            List<CosmeticRecommendationDto> cosmetics = new ArrayList<>();
            root.path("cosmetics").forEach(item -> cosmetics.add(
                    CosmeticRecommendationDto.builder()
                            .category(item.path("category").asText())
                            .productType(item.path("product_type").asText())
                            .recommendedColors(toStringList(item.path("recommended_colors")))
                            .finish(item.path("finish").asText())
                            .reason(item.path("reason").asText())
                            .build()
            ));
            List<OutfitRecommendationDto> outfits = new ArrayList<>();
            root.path("outfits").forEach(item -> outfits.add(
                    OutfitRecommendationDto.builder()
                            .style(item.path("style").asText())
                            .top(item.path("top").asText())
                            .bottom(item.path("bottom").asText())
                            .outerwear(item.path("outerwear").asText())
                            .shoes(item.path("shoes").asText())
                            .accessories(item.path("accessories").asText())
                            .colorPalette(toStringList(item.path("color_palette")))
                            .reason(item.path("reason").asText())
                            .build()
            ));
            if (cosmetics.isEmpty() && outfits.isEmpty()) {
                throw new IllegalStateException("AI 추천 결과가 비어 있습니다.");
            }
            return new AiPersonalColorResultDto(
                    root.path("summary").asText(), cosmetics, outfits
            );
        } catch (JacksonException exception) {
            throw new IllegalStateException("AI 퍼스널컬러 추천 응답을 처리할 수 없습니다.", exception);
        }
    }

    private List<String> toStringList(JsonNode node) {
        List<String> values = new ArrayList<>();
        node.forEach(value -> values.add(value.asText()));
        return values;
    }

    private JsonNode readJson(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (JacksonException exception) {
            throw new IllegalStateException("사용자 프로필을 처리할 수 없습니다.", exception);
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
            throw new IllegalStateException("OpenAI 응답에 추천 결과가 없습니다.");
        }
        return outputText.toString();
    }

    private String buildInstructions(boolean includeMakeup, boolean includeOutfits) {
        // 호출한 화면에 따라 옷/화장 중 하나만 답하도록 지침을 분리한다.
        if (includeMakeup && !includeOutfits) {
            return COMMON_INSTRUCTIONS + """

                    이번 요청은 화장 추천 전용입니다.
                    - 립, 블러셔, 아이섀도 등 최소 3개를 한국어로 추천하세요.
                    - 피부 타입과 피부 고민은 제품 질감 선택에만 보조적으로 사용하세요.
                    - outfits는 반드시 빈 배열로 반환하세요.
                    """;
        }
        if (!includeMakeup && includeOutfits) {
            return COMMON_INSTRUCTIONS + """

                    이번 요청은 오늘의 옷 추천 전용입니다.
                    - 상의, 하의, 아우터, 신발, 액세서리와 색상 팔레트를 포함한 코디를 최소 1개 추천하세요.
                    - 사용자의 message에서 일정, 장소, 날씨, 온도, 원하는 분위기를 파악해 옷차림에 반영하세요.
                    - message에 없는 조건을 임의로 만들어내지 마세요.
                    - cosmetics는 반드시 빈 배열로 반환하세요.
                    """;
        }
        return COMMON_INSTRUCTIONS + """

                색조 화장품 최소 3개와 오늘의 옷 코디 최소 1개를 각각 추천하세요.
                """;
    }
}
