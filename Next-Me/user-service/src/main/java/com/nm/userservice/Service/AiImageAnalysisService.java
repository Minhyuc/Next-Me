package com.nm.userservice.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nm.userservice.Dto.PhotoAnalysisResultDto;
import io.github.cdimascio.dotenv.Dotenv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AiImageAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(AiImageAnalysisService.class);

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper;
    private final Dotenv dotenv;

    public AiImageAnalysisService(ObjectMapper objectMapper, Dotenv dotenv) {
        this.objectMapper = objectMapper;
        this.dotenv = dotenv;
    }

    public PhotoAnalysisResultDto analyzeSkinTypePhoto(MultipartFile photo) {
        return analyzePhoto(photo, "skinType");
    }

    public PhotoAnalysisResultDto analyzePersonalColorPhoto(MultipartFile photo) {
        return analyzePhoto(photo, "personalColor");
    }

    private PhotoAnalysisResultDto analyzePhoto(MultipartFile photo, String target) {
        if (photo == null || photo.isEmpty()) {
            log.info("[OPENAI] photo가 없어서 이미지 분석을 건너뜁니다. target={}", target);
            return null;
        }

        String openAiApiKey = getEnvValue("OPENAI_API_KEY");
        String openAiModel = getEnvValue("OPENAI_MODEL", "gpt-4o-mini");
        String openAiEndpoint = getEnvValue("OPENAI_ENDPOINT", "https://api.openai.com/v1/chat/completions");

        if (!StringUtils.hasText(openAiApiKey)) {
            log.warn("[OPENAI] OPENAI_API_KEY가 없어 이미지 분석을 건너뜁니다.");
            return null;
        }

        try {
            String mimeType = StringUtils.hasText(photo.getContentType())
                    ? photo.getContentType()
                    : "image/jpeg";

            String base64Image = Base64.getEncoder().encodeToString(photo.getBytes());
            String dataUrl = "data:" + mimeType + ";base64," + base64Image;

            String prompt = buildPrompt(target);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", openAiModel);
            body.put("temperature", 0.3);
            body.put("response_format", Map.of("type", "json_object"));
            body.put("messages", List.of(
                    Map.of(
                            "role", "system",
                            "content", """
                                    You are an expert AI beauty analyst.
                                    
                                    You must analyze the uploaded image carefully.
                                    
                                    You MUST return ONLY valid JSON.
                                    
                                    You MUST NOT repeatedly return the same confidence score.
                                    
                                    The confidence score MUST realistically reflect the image quality and certainty of your prediction.
                                    
                                    Different images should naturally produce different confidence values.
                                    """
                    ),
                    Map.of(
                            "role", "user",
                            "content", List.of(
                                    Map.of(
                                            "type", "text",
                                            "text", prompt
                                    ),
                                    Map.of(
                                            "type", "image_url",
                                            "image_url", Map.of("url", dataUrl)
                                    )
                            )
                    )
            ));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(openAiApiKey);

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            log.info("[OPENAI] 이미지 분석 요청 시작. target={}, model={}, filename={}",
                    target, openAiModel, photo.getOriginalFilename());

            ResponseEntity<String> response = restTemplate.postForEntity(openAiEndpoint, requestEntity, String.class);

            log.info("[OPENAI] 응답 상태 코드: {}", response.getStatusCode());

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.warn("[OPENAI] 응답이 비정상적이거나 body가 없습니다. target={}", target);
                return null;
            }

            JsonNode root = objectMapper.readTree(response.getBody());

            JsonNode choices = root.path("choices");
            if (!choices.isArray() || choices.isEmpty()) {
                log.warn("[OPENAI] choices가 비어 있습니다. target={}, body={}", target, response.getBody());
                return null;
            }

            JsonNode messageNode = choices.get(0).path("message");
            String content = messageNode.path("content").asText(null);

            log.info("[OPENAI] 분석 결과 content={}", content);

            if (!StringUtils.hasText(content)) {
                log.warn("[OPENAI] content가 비어 있습니다. target={}", target);
                return null;
            }

            String normalizedJson = normalizeJson(content);
            JsonNode resultNode = objectMapper.readTree(normalizedJson);

            String skinType = resultNode.path("skinType").isMissingNode()
                    ? null
                    : resultNode.path("skinType").asText(null);

            String personalColor = resultNode.path("personalColor").isMissingNode()
                    ? null
                    : resultNode.path("personalColor").asText(null);

            String message = resultNode.path("message").isMissingNode()
                    ? null
                    : resultNode.path("message").asText(null);

            Integer confidence = resultNode.path("confidence").isMissingNode()
                    ? null
                    : resultNode.path("confidence").asInt();

            PhotoAnalysisResultDto result = PhotoAnalysisResultDto.builder()
                    .skinType(skinType)
                    .personalColor(personalColor)
                    .message(message)
                    .confidence(confidence)
                    .build();

            log.info("[OPENAI] 파싱 완료. target={}, skinType={}, personalColor={}, confidence={}, message={}",
                    target, skinType, personalColor, confidence, message);

            return result;

        } catch (Exception e) {
            log.error("[OPENAI] 이미지 분석 중 오류 발생. target={}, error={}", target, e.getMessage(), e);
            return null;
        }
    }

    private String buildPrompt(String target) {
        if ("skinType".equals(target)) {
            return """
                    You are an AI dermatologist for an MVP skincare service.
    
                    Your task is to estimate the user's MOST LIKELY skin type from a single face photo.

                    IMPORTANT

                    - Always make your best judgement.
                    - Never choose a confidence value simply because it is a common default.
                    - Evaluate every image independently.
                    - Different photos should naturally receive different confidence scores.
                    - Use the full range from 0 to 100 whenever appropriate.
                    - Use the entire confidence range from 0 to 100.
                    - Confidence must vary depending on the image quality AND how obvious the skin characteristics are.
                    - Images of different quality should produce different confidence values.

                    Skin types:

                    - 건성
                    - 지성
                    - 복합성
                    - 민감성
                    - 중성

                    Confidence rules:

                    95~100
                    - Excellent natural lighting
                    - Entire face visible
                    - High resolution
                    - Skin texture clearly visible
                    - Very obvious skin type

                    90~94
                    - Good lighting
                    - Slight uncertainty
                    - Most facial features visible

                    80~89
                    - Average image
                    - Some shadows
                    - Small ambiguity

                    70~79
                    - Indoor lighting
                    - Slight blur
                    - Harder to determine

                    50~69
                    - Poor lighting
                    - Significant ambiguity
    
                    0~49
                    - Face cannot be analyzed
                    - Too blurry
                    - Too dark
                    - Heavy filter
                    - Face not visible

                    Return ONLY JSON.

                    {
                      "skinType":"건성|지성|복합성|민감성|중성|null",
                      "personalColor":null,
                      "confidence":0,
                      "message":"한국어 한 문장"
                    }

                    Rules

                    - Confidence MUST reflect image quality.
                    - Do NOT repeatedly return 75.
                    - Prefer different confidence values such as 81, 84, 88, 91, 93, 96.
                    - Message must be in Korean.
                    - No markdown.
                    
                    The confidence score should not be arbitrary.
                    
                    It should realistically reflect:
                    - image quality
                    - visibility of facial features
                    - certainty of the classification
                    
                    Different photos should naturally produce different confidence values.
                    
                    Do not repeatedly return the same confidence score.
                    """;
        }

        if ("personalColor".equals(target)) {
            return """
                    You are a professional personal color consultant.
                    
                    Your task is to estimate the user's MOST LIKELY personal color from a single face photo.
                    
                    IMPORTANT
                    
                    - Always make your best judgement.
                    - Never choose a confidence value simply because it is a common default.
                    - Evaluate every image independently.
                    - Different photos should naturally receive different confidence scores.
                    - Use the full range from 0 to 100 whenever appropriate.
                    - Use confidence values realistically.
                    - Confidence should depend on:
                      - lighting
                      - skin visibility
                      - image resolution
                      - facial visibility
                      - color accuracy
                      - certainty of undertone
                    
                    Possible results
                    
                    - 봄 웜
                    - 여름 쿨
                    - 가을 웜
                    - 겨울 쿨
                    
                    Confidence rules
                    
                    95~100
                    - Excellent natural daylight
                    - Entire face visible
                    - Very clear undertone
                    - Excellent image quality
                    
                    90~94
                    - Good lighting
                    - Very likely season
                    
                    80~89
                    - Small ambiguity
                    - Minor shadows
                    
                    70~79
                    - Indoor lighting
                    - Average quality
                    - Hard to distinguish
                    
                    50~69
                    - Poor lighting
                    - Significant ambiguity
                    
                    0~49
                    - Impossible to determine
                    - Too dark
                    - Too blurry
                    - Heavy filter
                    - Face not visible
                    
                    Return ONLY JSON.
                    
                    {
                      "skinType":null,
                      "personalColor":"봄 웜|여름 쿨|가을 웜|겨울 쿨|null",
                      "confidence":0,
                      "message":"한국어 한 문장"
                    }
                    
                    Rules
                    
                    - Confidence MUST vary.
                    - Never always use 75.
                    - Different images should produce different confidence values.
                    - Use values such as 83, 87, 91, 94, 97 when appropriate.
                    - Message must be in Korean.
                    - No markdown.
                    
                    The confidence score should not be arbitrary.
                    
                    It should realistically reflect:
                    - image quality
                    - visibility of facial features
                    - certainty of the classification
                    
                    Different photos should naturally produce different confidence values.
                    
                    Do not repeatedly return the same confidence score.
                    """;
        }

        return """
                Analyze the uploaded image and return only JSON:
                {
                  "skinType": null,
                  "personalColor": null,
                  "confidence": 0,
                  "message": "짧은 설명"
                }
                """;
    }

    private String normalizeJson(String content) {
        String trimmed = content.trim();

        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceAll("^```(?:json)?\\s*", "");
            trimmed = trimmed.replaceAll("\\s*```$", "");
        }

        return trimmed.trim();
    }

    private String getEnvValue(String key) {
        String value = dotenv.get(key);
        if (!StringUtils.hasText(value)) {
            value = System.getenv(key);
        }
        return value;
    }

    private String getEnvValue(String key, String defaultValue) {
        String value = getEnvValue(key);
        return StringUtils.hasText(value) ? value : defaultValue;
    }
}