package com.nm.routineservice.Service;

import com.nm.routineservice.Dto.CharacterRewardResponseDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.LocalDate;
import java.util.Map;

@Service
/** 100% 완료된 루틴 날짜의 캐릭터 보상을 character-service에 요청한다. */
public class CharacterRewardClient {

    private final RestClient restClient;
    private final String internalKey;

    public CharacterRewardClient(
            @Value("${services.character-service.url}") String characterServiceUrl,
            @Value("${services.character-service.internal-key}") String internalKey
    ) {
        this.restClient = RestClient.builder().baseUrl(characterServiceUrl).build();
        this.internalKey = internalKey;
    }

    public CharacterRewardResponseDto reward(Long userId, LocalDate routineDate) {
        try {
            CharacterRewardResponseDto response = restClient.post()
                    .uri("/internal/characters/routine-completions")
                    .header("X-Internal-Service-Key", internalKey)
                    .body(Map.of(
                            "userId", userId,
                            "routineDate", routineDate.toString()
                    ))
                    .retrieve()
                    .body(CharacterRewardResponseDto.class);
            if (response == null) {
                throw new IllegalStateException("캐릭터 보상 응답이 비어 있습니다.");
            }
            return response;
        } catch (RestClientException exception) {
            // 보상이 누락된 채 완료만 확정되지 않도록 루틴 트랜잭션도 실패시킨다.
            throw new IllegalStateException(
                    "character-service에서 캐릭터 보상을 지급하지 못했습니다.", exception
            );
        }
    }
}
